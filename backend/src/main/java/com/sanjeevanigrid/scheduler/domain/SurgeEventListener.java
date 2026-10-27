package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.common.event.SurgePredicted;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event listener that reacts to SurgePredicted domain events emitted by the simulator.
 * Automatically activates surge-aware slot reservation for emergency intake.
 */
@Component
public class SurgeEventListener {

    private static final Logger log = LoggerFactory.getLogger(SurgeEventListener.class);

    private final SlotAllocationEngine slotAllocationEngine;
    private final DoctorRepository doctorRepository;
    private final NotificationGateway notificationGateway;

    private final AtomicBoolean surgeActive = new AtomicBoolean(false);
    private volatile SurgePredicted lastSurgeEvent;

    public SurgeEventListener(SlotAllocationEngine slotAllocationEngine,
                              DoctorRepository doctorRepository,
                              NotificationGateway notificationGateway) {
        this.slotAllocationEngine = slotAllocationEngine;
        this.doctorRepository = doctorRepository;
        this.notificationGateway = notificationGateway;
    }

    @EventListener
    public void onSurgePredicted(SurgePredicted event) {
        log.info("Received SurgePredicted event for ward '{}', peak={}%, depletion={}h, scenario='{}'",
                event.wardName(), event.peakOccupancyPct(), event.hoursUntilDepletion(), event.scenarioName());

        this.lastSurgeEvent = event;

        // Activate surge protocol if peak occupancy > 80% or depletion <= 12 hours
        if (event.peakOccupancyPct() > 80.0 || (event.hoursUntilDepletion() > 0 && event.hoursUntilDepletion() <= 12)) {
            activateSurgeProtocol(event);
        }
    }

    public synchronized int activateSurgeProtocol(SurgePredicted event) {
        surgeActive.set(true);
        LocalDate today = LocalDate.now();

        List<Doctor> doctors = doctorRepository.findAllByActiveTrue();
        int totalReserved = 0;

        for (Doctor doctor : doctors) {
            // Priority for General Medicine, Emergency, Pediatrics
            double ratio = (doctor.getSpecialty() == Specialty.EMERGENCY || doctor.getSpecialty() == Specialty.GENERAL_MEDICINE)
                    ? 0.35 : 0.20;
            totalReserved += slotAllocationEngine.reserveSurgeBuffer(doctor.getId(), today, ratio);
        }

        String alert = String.format(
                "CRITICAL SURGE ALERT: Surge protocol activated for %s. %d slots reserved across %d doctors for crisis intake. Depletion in %dh.",
                event != null ? event.wardName() : "Hospital", totalReserved, doctors.size(),
                event != null ? event.hoursUntilDepletion() : 9
        );
        log.warn(alert);
        notificationGateway.sendSms("ADMIN", alert);

        return totalReserved;
    }

    public synchronized int deactivateSurgeProtocol() {
        surgeActive.set(false);
        LocalDate today = LocalDate.now();
        List<Doctor> doctors = doctorRepository.findAllByActiveTrue();
        int totalReleased = 0;

        for (Doctor doctor : doctors) {
            totalReleased += slotAllocationEngine.releaseSurgeBuffer(doctor.getId(), today);
        }

        log.info("Surge protocol deactivated. Released {} reserved slots.", totalReleased);
        return totalReleased;
    }

    public boolean isSurgeActive() {
        return surgeActive.get();
    }

    public SurgePredicted getLastSurgeEvent() {
        return lastSurgeEvent;
    }
}
