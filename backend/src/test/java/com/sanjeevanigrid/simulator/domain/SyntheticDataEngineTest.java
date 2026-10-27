package com.sanjeevanigrid.simulator.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SyntheticDataEngine.
 */
class SyntheticDataEngineTest {

    private SyntheticDataEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SyntheticDataEngine(42L);
    }

    @Test
    @DisplayName("Generated data has exactly 2160 hourly points (90 days)")
    void generatesCorrectLength() {
        List<HourlyDataPoint> data = engine.generate(
                "General", 4.0, 50, 8, 4, 10, 5000, List.of());
        assertEquals(2160, data.size());
    }

    @Test
    @DisplayName("All data points have valid non-negative values")
    void dataPointsAreValid() {
        List<HourlyDataPoint> data = engine.generate(
                "General", 4.0, 50, 8, 4, 10, 5000, List.of());

        for (HourlyDataPoint dp : data) {
            assertTrue(dp.admissions() >= 0);
            assertTrue(dp.discharges() >= 0);
            assertTrue(dp.occupiedBeds() >= 0);
            assertTrue(dp.occupiedBeds() <= dp.totalBeds());
            assertTrue(dp.occupiedIcu() >= 0);
            assertTrue(dp.occupiedIcu() <= dp.totalIcu());
            assertTrue(dp.oxygenLevelPct() >= 0);
        }
    }

    @Test
    @DisplayName("Default hospital generates 4 wards")
    void defaultHospitalHas4Wards() {
        Map<String, List<HourlyDataPoint>> hospital = engine.generateDefaultHospital();
        assertEquals(4, hospital.size());
        assertTrue(hospital.containsKey("General"));
        assertTrue(hospital.containsKey("Pediatric"));
        assertTrue(hospital.containsKey("Maternity"));
        assertTrue(hospital.containsKey("Emergency"));
    }

    @Test
    @DisplayName("Deterministic seed produces reproducible data")
    void deterministicSeed() {
        SyntheticDataEngine engine1 = new SyntheticDataEngine(42L);
        SyntheticDataEngine engine2 = new SyntheticDataEngine(42L);

        List<HourlyDataPoint> data1 = engine1.generate("Test", 3.0, 30, 4, 2, 8, 2000, List.of());
        List<HourlyDataPoint> data2 = engine2.generate("Test", 3.0, 30, 4, 2, 8, 2000, List.of());

        for (int i = 0; i < data1.size(); i++) {
            assertEquals(data1.get(i).admissions(), data2.get(i).admissions());
        }
    }

    @Test
    @DisplayName("Surge events increase admissions during surge window")
    void surgeEventsIncreaseAdmissions() {
        List<SurgeEventDescriptor> surges = List.of(
                new SurgeEventDescriptor(100, 24, 5.0, "Test Surge"));

        List<HourlyDataPoint> withSurge = engine.generate(
                "Test", 3.0, 50, 5, 3, 8, 3000, surges);

        SyntheticDataEngine engine2 = new SyntheticDataEngine(42L);
        List<HourlyDataPoint> noSurge = engine2.generate(
                "Test", 3.0, 50, 5, 3, 8, 3000, List.of());

        // Sum admissions during surge window
        int surgeAdmissions = 0, normalAdmissions = 0;
        for (int h = 100; h < 124; h++) {
            surgeAdmissions += withSurge.get(h).admissions();
            normalAdmissions += noSurge.get(h).admissions();
        }

        assertTrue(surgeAdmissions > normalAdmissions,
                "Surge period should have more admissions");
    }
}
