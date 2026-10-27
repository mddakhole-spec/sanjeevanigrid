package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.AppointmentRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentSlotRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentWaitlistRepository;
import com.sanjeevanigrid.scheduler.infra.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * No-show auto-release service with waitlist promotion.
 * If an appointment is marked NO_SHOW, the slot is released.
 * If waitlisted patients exist, the next patient is automatically promoted and notified via SMS.
 */
@Service
public class NoShowService {

    private static final Logger log = LoggerFactory.getLogger(NoShowService.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentWaitlistRepository waitlistRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationGateway notificationGateway;

    public NoShowService(AppointmentRepository appointmentRepository,
                         AppointmentSlotRepository slotRepository,
                         AppointmentWaitlistRepository waitlistRepository,
                         DoctorRepository doctorRepository,
                         NotificationGateway notificationGateway) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.waitlistRepository = waitlistRepository;
        this.doctorRepository = doctorRepository;
        this.notificationGateway = notificationGateway;
    }

    /**
     * Process no-show for a specific appointment.
     */
    @Transactional
    public Optional<Appointment> processNoShow(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED && appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            log.warn("Cannot mark appointment {} as NO_SHOW because status is {}", appointmentId, appointment.getStatus());
            return Optional.empty();
        }

        appointment.markNoShow();
        appointmentRepository.save(appointment);
        log.info("Appointment {} for {} marked as NO_SHOW", appointmentId, appointment.getPatientName());

        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        String doctorName = doctor != null ? doctor.getFullName() : "Doctor";

        // Check if there is someone waiting on the waitlist
        List<AppointmentWaitlist> waitlist = waitlistRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByCreatedAtAsc(
                appointment.getDoctorId(), appointment.getAppointmentDate(), "WAITING");

        if (!waitlist.isEmpty()) {
            AppointmentWaitlist nextInLine = waitlist.get(0);
            nextInLine.promote();
            waitlistRepository.save(nextInLine);

            // Create new confirmed appointment for promoted patient
            String token = "T-WL" + (int)(Math.random() * 900 + 100);
            Appointment promotedAppointment = new Appointment(
                    appointment.getSlotId(),
                    appointment.getDoctorId(),
                    appointment.getHospitalId(),
                    nextInLine.getPatientId(),
                    nextInLine.getPatientName(),
                    nextInLine.getPatientPhone(),
                    null,
                    null,
                    "Promoted from waitlist after no-show",
                    nextInLine.getPriority(),
                    token,
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    false,
                    "WL-PROMOTE-" + nextInLine.getId()
            );
            promotedAppointment.setQueuePosition(appointment.getQueuePosition());
            promotedAppointment.setEstimatedWaitMins(appointment.getEstimatedWaitMins());
            Appointment saved = appointmentRepository.save(promotedAppointment);

            // Send SMS notification
            String msg = String.format(
                    "Great news! A slot opened with %s today at %s. Your appointment is CONFIRMED with Token %s. Please report to OPD room.",
                    doctorName, appointment.getStartTime(), token
            );
            notificationGateway.sendSms(nextInLine.getPatientPhone(), msg);
            log.info("Promoted waitlist patient {} to slot at {} with token {}", nextInLine.getPatientName(), appointment.getStartTime(), token);

            return Optional.of(saved);
        } else {
            // No one on waitlist: release slot back to available
            if (appointment.getSlotId() != null) {
                slotRepository.findById(appointment.getSlotId()).ifPresent(slot -> {
                    slot.release();
                    slotRepository.save(slot);
                    log.info("Slot {} released back to available", slot.getId());
                });
            }
            return Optional.empty();
        }
    }
}
