package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.scheduler.infra.AppointmentSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlotAllocationEngineTest {

    private AppointmentSlotRepository slotRepository;
    private SlotAllocationEngine engine;

    private Doctor doctor;
    private DoctorAvailability availability;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        slotRepository = mock(AppointmentSlotRepository.class);
        engine = new SlotAllocationEngine(slotRepository);

        doctor = new Doctor(
                UUID.randomUUID(), UUID.randomUUID(), "Dr. Priya Sharma",
                Specialty.GENERAL_MEDICINE, "MBBS, MD", 8,
                BigDecimal.ZERO, 30, 10
        );

        // 09:00 to 13:00, lunch break 11:00-12:00, 30-min slots
        availability = new DoctorAvailability(
                doctor.getId(), 1,
                LocalTime.of(9, 0), LocalTime.of(13, 0),
                LocalTime.of(11, 0), LocalTime.of(12, 0),
                30
        );

        testDate = LocalDate.of(2026, 11, 2); // Monday

        when(slotRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Should generate slots within working hours excluding lunch break")
    void testWorkingHoursAndBreakConstraint() {
        when(slotRepository.findAllByDoctorIdAndSlotDateOrderByStartTime(doctor.getId(), testDate))
                .thenReturn(List.of());

        List<AppointmentSlot> slots = engine.generateSlots(doctor, availability, testDate);

        // Slots should be: 09:00-09:30, 09:30-10:00, 10:00-10:30, 10:30-11:00,
        // (11:00-12:00 break skipped),
        // 12:00-12:30, 12:30-13:00
        // Total = 6 slots
        assertThat(slots).hasSize(6);

        // Verify none of the generated slots fall into 11:00-12:00 break
        for (AppointmentSlot slot : slots) {
            assertThat(slot.getStartTime()).isNotEqualTo(LocalTime.of(11, 0));
            assertThat(slot.getStartTime()).isNotEqualTo(LocalTime.of(11, 30));
        }

        assertThat(slots.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(slots.size() - 1).getEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    @DisplayName("Should respect max daily patients constraint")
    void testMaxDailyPatientsConstraint() {
        // Set doctor max to 3
        doctor.setMaxDailyPatients(3);

        when(slotRepository.findAllByDoctorIdAndSlotDateOrderByStartTime(doctor.getId(), testDate))
                .thenReturn(List.of());

        List<AppointmentSlot> slots = engine.generateSlots(doctor, availability, testDate);

        assertThat(slots).hasSize(3);
        assertThat(slots.get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1).getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slots.get(2).getStartTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("Should not duplicate existing slots (unique doctor+date+start constraint)")
    void testUniqueDoctorSlotConstraint() {
        AppointmentSlot existing = new AppointmentSlot(
                doctor.getId(), doctor.getHospitalId(), testDate,
                LocalTime.of(9, 0), LocalTime.of(9, 30)
        );

        when(slotRepository.findAllByDoctorIdAndSlotDateOrderByStartTime(doctor.getId(), testDate))
                .thenReturn(List.of(existing));

        List<AppointmentSlot> generated = engine.generateSlots(doctor, availability, testDate);

        // Should not contain another 09:00 slot
        long nineAmCount = generated.stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(9, 0)))
                .count();
        assertThat(nineAmCount).isEqualTo(0);
        assertThat(generated).hasSize(5); // 6 total minus existing 1 = 5 new
    }

    @Test
    @DisplayName("Should reserve surge buffer slots and lock routine bookings")
    void testSurgeReservationConstraints() {
        AppointmentSlot s1 = new AppointmentSlot(doctor.getId(), doctor.getHospitalId(), testDate, LocalTime.of(9, 0), LocalTime.of(9, 30));
        AppointmentSlot s2 = new AppointmentSlot(doctor.getId(), doctor.getHospitalId(), testDate, LocalTime.of(9, 30), LocalTime.of(10, 0));
        AppointmentSlot s3 = new AppointmentSlot(doctor.getId(), doctor.getHospitalId(), testDate, LocalTime.of(10, 0), LocalTime.of(10, 30));
        AppointmentSlot s4 = new AppointmentSlot(doctor.getId(), doctor.getHospitalId(), testDate, LocalTime.of(10, 30), LocalTime.of(11, 0));

        when(slotRepository.findByDoctorIdAndSlotDateAndStatusOrderByStartTime(doctor.getId(), testDate, SlotStatus.AVAILABLE))
                .thenReturn(List.of(s1, s2, s3, s4));
        when(slotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Reserve 25% buffer (1 slot)
        int reserved = engine.reserveSurgeBuffer(doctor.getId(), testDate, 0.25);
        assertThat(reserved).isEqualTo(1);
        assertThat(s1.isSurgeReserved()).isTrue();
        assertThat(s1.getStatus()).isEqualTo(SlotStatus.SURGE_RESERVED);

        // Test booking surge slot with routine priority -> throws exception
        UUID s1Id = UUID.randomUUID();
        when(slotRepository.findById(s1Id)).thenReturn(Optional.of(s1));

        assertThatThrownBy(() -> engine.reserveSlot(s1Id, AppointmentPriority.ROUTINE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserved for emergency crisis intake");

        // Test booking surge slot with emergency priority -> succeeds!
        AppointmentSlot booked = engine.reserveSlot(s1Id, AppointmentPriority.EMERGENCY);
        assertThat(booked.getStatus()).isEqualTo(SlotStatus.BOOKED);
    }
}
