package com.sanjeevanigrid.simulator.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioEngine.
 */
class ScenarioEngineTest {

    private ScenarioEngine engine;
    private List<HourlyDataPoint> baselineData;

    @BeforeEach
    void setUp() {
        engine = new ScenarioEngine();
        SyntheticDataEngine dataEngine = new SyntheticDataEngine(42L);
        baselineData = dataEngine.generate("General", 4.0, 60, 8, 4, 12, 5000, List.of());
    }

    @Test
    @DisplayName("Normal scenario produces valid KPIs")
    void normalScenarioProducesValidKPIs() {
        ScenarioParams params = ScenarioEngine.normalPreset();
        ScenarioResult result = engine.run(baselineData, params);

        assertNotNull(result.kpis());
        assertTrue(result.kpis().peakOccupancyPct() >= 0);
        assertTrue(result.kpis().patientsTurnedAway() >= 0);
        assertTrue(result.kpis().hoursAbove90Pct() >= 0);
    }

    @Test
    @DisplayName("Flood preset produces higher occupancy than normal")
    void floodPresetProducesHigherOccupancy() {
        ScenarioResult normal = engine.run(baselineData, ScenarioEngine.normalPreset());
        ScenarioResult flood = engine.run(baselineData, ScenarioEngine.monsoonFloodPreset());

        assertTrue(flood.kpis().peakOccupancyPct() >= normal.kpis().peakOccupancyPct(),
                "Flood should produce higher peak occupancy");
    }

    @Test
    @DisplayName("STRICT triage reduces admissions vs NORMAL")
    void strictTriageReducesLoad() {
        ScenarioParams normalTriage = new ScenarioParams(3.0, "NORMAL", 0, 0, 72, "Normal");
        ScenarioParams strictTriage = new ScenarioParams(3.0, "STRICT", 0, 0, 72, "Strict");

        ScenarioResult normalResult = engine.run(baselineData, normalTriage);
        ScenarioResult strictResult = engine.run(baselineData, strictTriage);

        assertTrue(strictResult.kpis().peakOccupancyPct() <= normalResult.kpis().peakOccupancyPct(),
                "STRICT triage should produce lower or equal peak occupancy");
    }

    @Test
    @DisplayName("Extra beds reduce patients turned away")
    void extraBedsReduceTurnedAway() {
        ScenarioParams noBeds = new ScenarioParams(3.5, "NORMAL", 0, 0, 72, "No extra");
        ScenarioParams withBeds = new ScenarioParams(3.5, "NORMAL", 30, 0, 72, "Extra beds");

        ScenarioResult noResult = engine.run(baselineData, noBeds);
        ScenarioResult withResult = engine.run(baselineData, withBeds);

        assertTrue(withResult.kpis().patientsTurnedAway() <= noResult.kpis().patientsTurnedAway(),
                "Extra beds should reduce or maintain patients turned away");
    }

    @Test
    @DisplayName("Simulation duration matches requested hours")
    void durationMatchesRequested() {
        ScenarioParams params = new ScenarioParams(1.0, "NORMAL", 0, 0, 48, "Test");
        ScenarioResult result = engine.run(baselineData, params);
        assertEquals(48, result.hours().size());
    }

    @Test
    @DisplayName("Recommendations are generated for high-occupancy scenarios")
    void recommendationsGenerated() {
        ScenarioParams surge = new ScenarioParams(4.0, "NORMAL", 0, 0, 72, "Surge");
        ScenarioResult result = engine.run(baselineData, surge);

        assertFalse(result.recommendations().isEmpty(),
                "Should produce recommendations");
        assertTrue(result.recommendations().stream().anyMatch(r -> r.contains("forecast")),
                "Should include the disclaimer");
    }
}
