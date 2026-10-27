package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.common.event.AppointmentShifted;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.AppointmentRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentWaitlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Emergency Bump Service.
 * When emergency appointments arrive and all slots are full, bumps the lowest-priority
 * routine appointment, shifts the slot to the emergency patient, places the bumped patient
 * at the top of the priority waitlist, emits AppointmentShifted domain event, and sends an SMS alert.
 */
@Service
public class EmergencyBumpService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyBumpService.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentWaitlistRepository waitlistRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationGateway notificationGateway;

    public EmergencyBumpService(AppointmentRepository appointmentRepository,
                                AppointmentWaitlistRepository waitlistRepository,
                                ApplicationEventPublisher eventPublisher,
                                NotificationGateway notificationGateway) {
        this.appointmentRepository = appointmentRepository;
        this.waitlistRepository = waitlistRepository;
        this.eventPublisher = eventPublisher;
        this.notificationGateway = notificationGateway;
    }

    /**
     * Atomically bump the lowest-priority appointment to make room for an emergency appointment.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized Appointment bumpForEmergency(Doctor doctor, LocalDate date, Appointment emergencyAppointment) {
        List<Appointment> existing = appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusInOrderByStartTimeAsc(
                doctor.getId(), date, List.of(AppointmentStatus.CONFIRMED));

        // Find candidate with lowest priority score (Routine < Urgent < Emergency)
        // If tied, pick the one furthest down or matching slot
        Optional<Appointment> candidateOpt = existing.stream()
                .filter(a -> emergencyAppointment.getPriority().canBump(a.getPriority()))
                .min(Comparator.comparingInt(Appointment::getPriorityScore)
                        .thenComparing(Appointment::getStartTime, Comparator.reverseOrder()));

        if (candidateOpt.isEmpty()) {
            throw new IllegalStateException("Capacity full: No lower-priority routine appointment found to bump for emergency patient.");
        }

        Appointment bumped = candidateOpt.get();

        // 1. Mark bumped
        String reason = "Rescheduled due to critical emergency intake (Token " + emergencyAppointment.getTokenNumber() + ")";
        bumped.bump(reason);
        appointmentRepository.save(bumped);

        // 2. Add bumped patient to top priority waitlist
        AppointmentWaitlist waitlistEntry = new AppointmentWaitlist(
                doctor.getId(),
                date,
                bumped.getPatientId(),
                bumped.getPatientName(),
                bumped.getPatientPhone(),
                bumped.getPriority()
        );
        waitlistRepository.save(waitlistEntry);

        // 3. Assign the bumped slot to emergency appointment
        emergencyAppointment.setSlotId(bumped.getSlotId());
        emergencyAppointment.setStartTime(bumped.getStartTime());
        emergencyAppointment.setEndTime(bumped.getEndTime());
        emergencyAppointment.setQueuePosition(Math.max(1, bumped.getQueuePosition()));

        // 4. Publish AppointmentShifted domain event
        AppointmentShifted event = new AppointmentShifted(
                bumped.getId(),
                doctor.getId(),
                doctor.getFullName(),
                bumped.getPatientName(),
                bumped.getPatientPhone(),
                date,
                bumped.getStartTime(),
                reason,
                "BUMPED"
        );
        eventPublisher.publishEvent(event);

        // 5. Send SMS alert via NotificationGateway (Sandbox SMS / In-App)
        String smsMessage = String.format(
                "URGENT: SanjeevaniGrid Notice: Your appointment with %s on %s at %s has been rescheduled due to a life-threatening emergency trauma intake. You have been placed at #1 priority on our immediate waitlist.",
                doctor.getFullName(), date, bumped.getStartTime()
        );
        notificationGateway.sendSms(bumped.getPatientPhone(), smsMessage);

        log.info("Emergency patient {} bumped appointment {} for patient {} with doctor {}",
                emergencyAppointment.getTokenNumber(), bumped.getId(), bumped.getPatientName(), doctor.getFullName());

        return bumped;
    }
}
