package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.domain.Appointment;
import com.sanjeevanigrid.scheduler.domain.AppointmentStatus;
import com.sanjeevanigrid.scheduler.domain.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled task that dispatches appointment reminders via SMS / In-app notifications.
 * Scans upcoming appointments within the next 2 hours or due for reminder.
 */
@Component
public class AppointmentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationGateway notificationGateway;

    public AppointmentReminderScheduler(AppointmentRepository appointmentRepository,
                                        DoctorRepository doctorRepository,
                                        NotificationGateway notificationGateway) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.notificationGateway = notificationGateway;
    }

    /**
     * Run every 60 seconds (or on schedule) to dispatch pending reminders.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void sendUpcomingReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime windowEnd = now.plusHours(2);

        List<Appointment> pending = appointmentRepository.findAllByStatusAndAppointmentDateAndReminderSentFalse(
                AppointmentStatus.CONFIRMED, today);

        int sentCount = 0;
        for (Appointment appt : pending) {
            // If appointment start time is within the next 2 hours or today
            if (appt.getStartTime().isBefore(windowEnd)) {
                String doctorName = doctorRepository.findById(appt.getDoctorId())
                        .map(Doctor::getFullName)
                        .orElse("Your Doctor");

                String message = String.format(
                        "Reminder from SanjeevaniGrid: Your appointment with %s is scheduled for today at %s. Token #%s. OPD Room 2, Yavatmal District Hospital.",
                        doctorName, appt.getStartTime(), appt.getTokenNumber()
                );

                notificationGateway.sendSms(appt.getPatientPhone(), message);
                appt.setReminderSent(true);
                appointmentRepository.save(appt);
                sentCount++;
            }
        }

        if (sentCount > 0) {
            log.info("Dispatched {} appointment reminder notifications.", sentCount);
        }
    }
}
