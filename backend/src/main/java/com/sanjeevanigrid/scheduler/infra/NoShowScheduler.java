package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.scheduler.domain.Appointment;
import com.sanjeevanigrid.scheduler.domain.AppointmentStatus;
import com.sanjeevanigrid.scheduler.domain.NoShowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled task that detects missed check-ins (15+ minutes past appointment start time),
 * automatically marks them as NO_SHOW, releases the slot, and promotes any waiting patient.
 */
@Component
public class NoShowScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoShowScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final NoShowService noShowService;

    public NoShowScheduler(AppointmentRepository appointmentRepository, NoShowService noShowService) {
        this.appointmentRepository = appointmentRepository;
        this.noShowService = noShowService;
    }

    /**
     * Check for expired check-ins every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void scanAndReleaseNoShows() {
        LocalDate today = LocalDate.now();
        LocalTime graceCutoff = LocalTime.now().minusMinutes(15);

        List<Appointment> overdue = appointmentRepository.findAllByStatusAndAppointmentDateAndStartTimeBefore(
                AppointmentStatus.CONFIRMED, today, graceCutoff);

        for (Appointment appt : overdue) {
            log.info("Appointment {} for {} is overdue past check-in cutoff {}. Triggering auto-release.",
                    appt.getId(), appt.getPatientName(), graceCutoff);
            noShowService.processNoShow(appt.getId());
        }
    }
}
