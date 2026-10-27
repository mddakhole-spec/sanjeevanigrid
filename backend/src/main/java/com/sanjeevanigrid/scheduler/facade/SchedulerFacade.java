package com.sanjeevanigrid.scheduler.facade;

import com.sanjeevanigrid.common.event.SurgePredicted;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.domain.*;
import com.sanjeevanigrid.scheduler.infra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Scheduler module facade – the public API surface for Service Coordination.
 */
@Service
public class SchedulerFacade {

    private static final Logger log = LoggerFactory.getLogger(SchedulerFacade.class);

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotAllocationEngine slotAllocationEngine;
    private final EmergencyBumpService emergencyBumpService;
    private final WaitTimePredictor waitTimePredictor;
    private final NoShowService noShowService;
    private final SurgeEventListener surgeEventListener;
    private final NotificationGateway notificationGateway;

    public SchedulerFacade(DoctorRepository doctorRepository,
                           DoctorAvailabilityRepository availabilityRepository,
                           AppointmentSlotRepository slotRepository,
                           AppointmentRepository appointmentRepository,
                           SlotAllocationEngine slotAllocationEngine,
                           EmergencyBumpService emergencyBumpService,
                           WaitTimePredictor waitTimePredictor,
                           NoShowService noShowService,
                           SurgeEventListener surgeEventListener,
                           NotificationGateway notificationGateway) {
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.slotAllocationEngine = slotAllocationEngine;
        this.emergencyBumpService = emergencyBumpService;
        this.waitTimePredictor = waitTimePredictor;
        this.noShowService = noShowService;
        this.surgeEventListener = surgeEventListener;
        this.notificationGateway = notificationGateway;
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAllByActiveTrue();
    }

    public List<Doctor> getDoctorsBySpecialty(Specialty specialty) {
        return doctorRepository.findAllBySpecialtyAndActiveTrue(specialty);
    }

    public Optional<Doctor> getDoctorById(UUID id) {
        return doctorRepository.findById(id);
    }

    public List<DoctorAvailability> getDoctorAvailability(UUID doctorId) {
        return availabilityRepository.findAllByDoctorId(doctorId);
    }

    @Transactional
    public List<DoctorAvailability> updateDoctorAvailability(UUID doctorId, List<DoctorAvailability> availabilities) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        List<DoctorAvailability> existing = availabilityRepository.findAllByDoctorId(doctorId);
        availabilityRepository.deleteAll(existing);

        for (DoctorAvailability a : availabilities) {
            a.setDoctorId(doctorId);
        }
        return availabilityRepository.saveAll(availabilities);
    }

    @Transactional
    public List<AppointmentSlot> getOrCreateSlots(UUID doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        List<AppointmentSlot> slots = slotRepository.findAllByDoctorIdAndSlotDateOrderByStartTime(doctorId, date);
        if (!slots.isEmpty()) {
            return slots;
        }

        // Generate slots from availability template for day of week
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun
        Optional<DoctorAvailability> availOpt = availabilityRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);

        if (availOpt.isPresent()) {
            return slotAllocationEngine.generateSlots(doctor, availOpt.get(), date);
        } else {
            // Default 9am-5pm with 1-2pm lunch break if no template set
            DoctorAvailability defaultAvail = new DoctorAvailability(
                    doctorId, dayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0),
                    LocalTime.of(13, 0), LocalTime.of(14, 0), doctor.getConsultationDurationMins()
            );
            return slotAllocationEngine.generateSlots(doctor, defaultAvail, date);
        }
    }

    /**
     * Book an appointment with wizard data, idempotency, wait-time prediction,
     * and emergency priority bump if slots are full.
     */
    @Transactional
    public Appointment bookAppointment(UUID doctorId, UUID slotId, String patientName,
                                       String patientPhone, Integer patientAge, String patientGender,
                                       String chiefComplaint, AppointmentPriority priority,
                                       LocalDate appointmentDate, String idempotencyKey) {

        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Appointment> existing = appointmentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent booking hit for key {}", idempotencyKey);
                return existing.get();
            }
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        AppointmentPriority reqPriority = priority != null ? priority : AppointmentPriority.ROUTINE;
        boolean isEmergency = reqPriority == AppointmentPriority.EMERGENCY;

        AppointmentSlot slot = null;
        if (slotId != null) {
            slot = slotRepository.findById(slotId).orElse(null);
        }

        // Generate token
        String prefix = isEmergency ? "E-" : (reqPriority == AppointmentPriority.URGENT ? "U-" : "T-");
        long countToday = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusIn(
                doctorId, appointmentDate, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN,
                        AppointmentStatus.IN_CONSULTATION, AppointmentStatus.COMPLETED));
        String tokenNumber = prefix + (101 + countToday);

        Appointment appointment = new Appointment(
                slot != null ? slot.getId() : null,
                doctorId,
                doctor.getHospitalId(),
                null,
                patientName,
                patientPhone,
                patientAge,
                patientGender,
                chiefComplaint,
                reqPriority,
                tokenNumber,
                appointmentDate,
                slot != null ? slot.getStartTime() : LocalTime.of(9, 0),
                slot != null ? slot.getEndTime() : LocalTime.of(9, 15),
                false,
                idempotencyKey
        );

        if (isEmergency) {
            // Check if selected slot or any slot is available
            boolean bookedOpenSlot = false;
            if (slot != null && slot.isAvailableForEmergency()) {
                try {
                    slotAllocationEngine.reserveSlot(slot.getId(), reqPriority);
                    bookedOpenSlot = true;
                } catch (Exception e) {
                    log.warn("Emergency patient slot reservation threw: {}. Attempting bump logic.", e.getMessage());
                }
            }

            if (!bookedOpenSlot) {
                // Find any open slot
                List<AppointmentSlot> openSlots = slotRepository.findByDoctorIdAndSlotDateAndStatusOrderByStartTime(
                        doctorId, appointmentDate, SlotStatus.AVAILABLE);
                if (!openSlots.isEmpty()) {
                    AppointmentSlot openSlot = openSlots.get(0);
                    slotAllocationEngine.reserveSlot(openSlot.getId(), reqPriority);
                    appointment.setSlotId(openSlot.getId());
                    appointment.setStartTime(openSlot.getStartTime());
                    appointment.setEndTime(openSlot.getEndTime());
                } else {
                    // Slots full: BUMP lowest priority routine appointment!
                    emergencyBumpService.bumpForEmergency(doctor, appointmentDate, appointment);
                }
            }
        } else {
            // Routine / Urgent booking
            if (slot == null) {
                throw new IllegalArgumentException("Slot must be specified for non-emergency bookings.");
            }
            slotAllocationEngine.reserveSlot(slot.getId(), reqPriority);
            appointment.setSlotId(slot.getId());
            appointment.setStartTime(slot.getStartTime());
            appointment.setEndTime(slot.getEndTime());
        }

        // Calculate queue position & predicted wait time
        int queuePosition = (int) countToday;
        int waitMins = waitTimePredictor.predictWaitTimeMins(queuePosition, doctor.getRollingAvgConsultationMins());
        appointment.setQueuePosition(queuePosition);
        appointment.setEstimatedWaitMins(waitMins);

        Appointment saved = appointmentRepository.save(appointment);

        // Send booking confirmation SMS
        String msg = String.format(
                "SanjeevaniGrid: Appointment CONFIRMED with %s on %s at %s. Token #%s. Queue pos: %d (Est. wait: %dm).",
                doctor.getFullName(), appointmentDate, saved.getStartTime(), saved.getTokenNumber(),
                saved.getQueuePosition(), saved.getEstimatedWaitMins()
        );
        notificationGateway.sendSms(patientPhone, msg);

        return saved;
    }

    /**
     * Issue an immediate walk-in token at the clinic/OPD desk.
     */
    @Transactional
    public Appointment createWalkInToken(UUID doctorId, String patientName, String patientPhone,
                                         AppointmentPriority priority, String chiefComplaint) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        long countToday = appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusIn(
                doctorId, today, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN,
                        AppointmentStatus.IN_CONSULTATION, AppointmentStatus.COMPLETED));

        String token = "W-" + (201 + countToday);
        AppointmentPriority reqPriority = priority != null ? priority : AppointmentPriority.ROUTINE;

        Appointment walkIn = new Appointment(
                null,
                doctorId,
                doctor.getHospitalId(),
                null,
                patientName,
                patientPhone,
                null,
                null,
                chiefComplaint,
                reqPriority,
                token,
                today,
                now,
                now.plusMinutes(doctor.getConsultationDurationMins()),
                true,
                "WALKIN-" + UUID.randomUUID()
        );

        int queuePosition = (int) countToday + 1;
        int waitMins = waitTimePredictor.predictWaitTimeMins(queuePosition, doctor.getRollingAvgConsultationMins());
        walkIn.setQueuePosition(queuePosition);
        walkIn.setEstimatedWaitMins(waitMins);
        walkIn.setStatus(AppointmentStatus.CHECKED_IN);
        walkIn.setCheckedInAt(java.time.Instant.now());

        Appointment saved = appointmentRepository.save(walkIn);

        notificationGateway.sendSms(patientPhone, String.format(
                "SanjeevaniGrid Walk-In: Token %s issued for %s. Queue pos: %d. Estimated wait: %d mins.",
                token, doctor.getFullName(), queuePosition, waitMins
        ));

        return saved;
    }

    /**
     * Get live OPD queue screen state for a doctor on a specific date.
     */
    public Map<String, Object> getLiveQueue(UUID doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        List<Appointment> allToday = appointmentRepository.findAllByDoctorIdAndAppointmentDateOrderByQueuePositionAscStartTimeAsc(doctorId, date);

        Appointment nowServing = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.IN_CONSULTATION)
                .findFirst()
                .orElse(allToday.stream().filter(a -> a.getStatus() == AppointmentStatus.CHECKED_IN).findFirst().orElse(null));

        List<Appointment> upcoming = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED || (a.getStatus() == AppointmentStatus.CHECKED_IN && a != nowServing))
                .toList();

        List<Appointment> completed = allToday.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("doctor", doctor);
        response.put("date", date);
        response.put("nowServing", nowServing);
        response.put("upcoming", upcoming);
        response.put("completedCount", completed.size());
        response.put("totalQueue", upcoming.size() + (nowServing != null ? 1 : 0));
        response.put("rollingAvgMins", doctor.getRollingAvgConsultationMins());
        response.put("isSurgeActive", surgeEventListener.isSurgeActive());
        response.put("lastSurgeEvent", surgeEventListener.getLastSurgeEvent());

        return response;
    }

    /**
     * Advance OPD queue: Call Next, Mark Done, or No-Show.
     */
    @Transactional
    public Appointment advanceQueue(UUID appointmentId, String action, Double actualDurationMins) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        Doctor doctor = doctorRepository.findById(appt.getDoctorId()).orElse(null);

        switch (action.toUpperCase()) {
            case "CHECK_IN" -> appt.checkIn();
            case "START", "CALL_NEXT" -> appt.startConsultation();
            case "COMPLETE" -> {
                double duration = appt.completeConsultation();
                if (actualDurationMins != null && actualDurationMins > 0) duration = actualDurationMins;
                if (doctor != null) {
                    doctor.recordConsultationDuration(duration);
                    doctorRepository.save(doctor);
                }
            }
            case "NO_SHOW" -> {
                noShowService.processNoShow(appointmentId);
                return appointmentRepository.findById(appointmentId).orElse(appt);
            }
            case "CANCEL" -> appt.cancel();
            default -> throw new IllegalArgumentException("Unknown queue action: " + action);
        }

        return appointmentRepository.save(appt);
    }

    public int triggerSurgeProtocol(String wardName, int hoursUntilDepletion) {
        SurgePredicted event = new SurgePredicted(
                wardName != null ? wardName : "Emergency & General Medicine",
                "Monsoon Surge / Crisis",
                89.5,
                hoursUntilDepletion > 0 ? hoursUntilDepletion : 9,
                "beds",
                42
        );
        return surgeEventListener.activateSurgeProtocol(event);
    }

    public int deactivateSurgeProtocol() {
        return surgeEventListener.deactivateSurgeProtocol();
    }
}
