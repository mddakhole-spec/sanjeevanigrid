package com.sanjeevanigrid.camps.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VaccineScheduleEngineTest {

    private VaccineScheduleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new VaccineScheduleEngine();
    }

    @Test
    @DisplayName("Newborn at birth has birth doses due now and future doses upcoming")
    void testNewbornSchedule() {
        LocalDate dob = LocalDate.now();
        VaccineScheduleEngine.ScheduleResult result = engine.evaluateSchedule(dob, List.of());

        assertNotNull(result);
        assertEquals(0, result.ageInMonths());
        assertTrue(result.dueNowCount() >= 2, "Birth doses like BCG and HepB should be due now");
        assertTrue(result.upcomingCount() > 5, "Future doses should be marked UPCOMING");
        assertEquals(0, result.completedCount());

        // Check BCG is DUE_NOW
        boolean bcgDue = result.schedule().stream()
                .anyMatch(v -> "BCG".equals(v.vaccineName()) && "DUE_NOW".equals(v.status()));
        assertTrue(bcgDue, "BCG should be DUE_NOW at birth");
    }

    @Test
    @DisplayName("Child with completed birth doses shows COMPLETED for those doses")
    void testCompletedDoses() {
        LocalDate dob = LocalDate.now().minusWeeks(6);
        List<VaccineScheduleEngine.AdministeredDose> history = List.of(
                new VaccineScheduleEngine.AdministeredDose("BCG", 1, dob),
                new VaccineScheduleEngine.AdministeredDose("HepB_Birth", 1, dob),
                new VaccineScheduleEngine.AdministeredDose("OPV", 0, dob)
        );

        VaccineScheduleEngine.ScheduleResult result = engine.evaluateSchedule(dob, history);

        assertEquals(3, result.completedCount());
        assertTrue(result.schedule().stream()
                .anyMatch(v -> "BCG".equals(v.vaccineName()) && "COMPLETED".equals(v.status())));
    }

    @Test
    @DisplayName("Child at 6 months who missed 6-week and 10-week vaccines is flagged as OVERDUE")
    void testOverdueVaccines() {
        LocalDate dob = LocalDate.now().minusMonths(6);
        VaccineScheduleEngine.ScheduleResult result = engine.evaluateSchedule(dob, List.of());

        assertTrue(result.overdueCount() > 0, "Should have multiple overdue vaccines for unimmunized 6-month infant");
        boolean pentaOverdue = result.schedule().stream()
                .anyMatch(v -> "Pentavalent".equals(v.vaccineName()) && v.doseNumber() == 1 && "OVERDUE".equals(v.status()));
        assertTrue(pentaOverdue, "Pentavalent 1 should be OVERDUE at 6 months if not administered");
    }

    @Test
    @DisplayName("Dose 2 calculates minimum 28 days interval after Dose 1")
    void testMinimumIntervalPrerequisite() {
        LocalDate dob = LocalDate.now().minusWeeks(12);
        LocalDate penta1Date = LocalDate.now().minusDays(10); // given 10 days ago (delayed)

        List<VaccineScheduleEngine.AdministeredDose> history = List.of(
                new VaccineScheduleEngine.AdministeredDose("Pentavalent", 1, penta1Date)
        );

        VaccineScheduleEngine.ScheduleResult result = engine.evaluateSchedule(dob, history);

        VaccineScheduleEngine.VaccineStatus penta2 = result.schedule().stream()
                .filter(v -> "Pentavalent".equals(v.vaccineName()) && v.doseNumber() == 2)
                .findFirst()
                .orElseThrow();

        assertEquals("UPCOMING", penta2.status(), "Pentavalent 2 should be UPCOMING until 28 days pass after dose 1");
        assertEquals(penta1Date.plusDays(28), penta2.nextDueDate());
    }
}

