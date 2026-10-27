package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.scheduler.infra.AppointmentSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Constraint-based slot allocation and management engine.
 * Enforces:
 *  - Working hours
 *  - Breaks (lunch / rest)
 *  - Max daily patients limit
 *  - Unique (doctor, date, start_time) constraint
 *  - Optimistic locking via @Version
 *  - Surge buffer reservation
 */
@Service
public class SlotAllocationEngine {

    private static final Logger log = LoggerFactory.getLogger(SlotAllocationEngine.class);

    private final AppointmentSlotRepository slotRepository;

    public SlotAllocationEngine(AppointmentSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    /**
     * Generate daily slots for a doctor based on their weekly availability template.
     */
    @Transactional
    public List<AppointmentSlot> generateSlots(Doctor doctor, DoctorAvailability availability, LocalDate date) {
        if (!availability.isActive() || !doctor.isActive()) {
            return List.of();
        }

        int duration = availability.getSlotDurationMins() > 0 ? availability.getSlotDurationMins() : doctor.getConsultationDurationMins();
        int maxSlots = doctor.getMaxDailyPatients();

        List<AppointmentSlot> newSlots = new ArrayList<>();
        LocalTime current = availability.getStartTime();
        LocalTime end = availability.getEndTime();

        // Get existing slots for the date to avoid duplicates
        List<AppointmentSlot> existingSlots = slotRepository.findAllByDoctorIdAndSlotDateOrderByStartTime(doctor.getId(), date);
        int currentCount = existingSlots.size();

        while (current.plusMinutes(duration).compareTo(end) <= 0) {
            LocalTime slotEnd = current.plusMinutes(duration);

            // Constraint 1: Skip if overlaps with break
            if (!availability.isInBreak(current) && (availability.getBreakStartTime() == null || current.compareTo(availability.getBreakStartTime()) < 0 || current.compareTo(availability.getBreakEndTime()) >= 0)) {
                // Constraint 2: Max daily patients
                if (currentCount + newSlots.size() >= maxSlots) {
                    log.info("Reached doctor {} max daily capacity of {} slots for date {}", doctor.getFullName(), maxSlots, date);
                    break;
                }

                // Constraint 3: Unique doctor + date + start time
                final LocalTime startTime = current;
                boolean alreadyExists = existingSlots.stream().anyMatch(s -> s.getStartTime().equals(startTime));
                if (!alreadyExists) {
                    AppointmentSlot slot = new AppointmentSlot(
                            doctor.getId(),
                            doctor.getHospitalId(),
                            date,
                            startTime,
                            slotEnd
                    );
                    newSlots.add(slot);
                }
            }

            current = slotEnd;
        }

        if (!newSlots.isEmpty()) {
            List<AppointmentSlot> saved = slotRepository.saveAll(newSlots);
            log.info("Generated {} slots for doctor {} on {}", saved.size(), doctor.getFullName(), date);
            return saved;
        }

        return existingSlots;
    }

    /**
     * Attempt to reserve a specific slot with optimistic locking.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized AppointmentSlot reserveSlot(UUID slotId, AppointmentPriority priority) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new IllegalStateException("Slot " + slotId + " is already booked.");
        }

        // Surge reservation constraint: routine patients cannot book surge-reserved slots
        if (slot.isSurgeReserved() && (priority == null || priority != AppointmentPriority.EMERGENCY)) {
            throw new IllegalStateException("Slot is reserved for emergency crisis intake. Routine bookings locked.");
        }

        slot.book();
        return slotRepository.save(slot);
    }

    /**
     * Reserve a surge buffer (e.g., 25% of open slots) for crisis intake.
     */
    @Transactional
    public int reserveSurgeBuffer(UUID doctorId, LocalDate date, double fraction) {
        List<AppointmentSlot> available = slotRepository.findByDoctorIdAndSlotDateAndStatusOrderByStartTime(
                doctorId, date, SlotStatus.AVAILABLE);

        if (available.isEmpty()) return 0;

        int toReserve = (int) Math.ceil(available.size() * fraction);
        int reservedCount = 0;

        for (int i = 0; i < toReserve && i < available.size(); i++) {
            AppointmentSlot slot = available.get(i);
            slot.markSurgeReserved(true);
            slotRepository.save(slot);
            reservedCount++;
        }

        log.info("Surge buffer reserved {}/{} slots for doctor {} on {}", reservedCount, available.size(), doctorId, date);
        return reservedCount;
    }

    /**
     * Release surge buffer slots back to general availability.
     */
    @Transactional
    public int releaseSurgeBuffer(UUID doctorId, LocalDate date) {
        List<AppointmentSlot> surgeSlots = slotRepository.findAllByDoctorIdAndSlotDateAndIsSurgeReservedTrue(doctorId, date);
        int releasedCount = 0;

        for (AppointmentSlot slot : surgeSlots) {
            if (slot.getStatus() == SlotStatus.SURGE_RESERVED) {
                slot.markSurgeReserved(false);
                slotRepository.save(slot);
                releasedCount++;
            }
        }

        log.info("Released {} surge-reserved slots for doctor {} on {}", releasedCount, doctorId, date);
        return releasedCount;
    }
}
