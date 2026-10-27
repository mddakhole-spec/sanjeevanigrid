package com.sanjeevanigrid.simulator.facade;

import com.sanjeevanigrid.common.event.SurgePredicted;
import com.sanjeevanigrid.simulator.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simulator module facade – the only public API surface of the simulator module.
 * Other modules interact exclusively through this facade.
 */
@Service
public class SimulatorFacade {

    private static final Logger log = LoggerFactory.getLogger(SimulatorFacade.class);

    private final ApplicationEventPublisher eventPublisher;
    private final SyntheticDataEngine dataEngine;
    private final HoltWintersForecaster forecaster;
    private final ScenarioEngine scenarioEngine;

    // Cached baseline data (generated once, deterministic seed)
    private Map<String, List<HourlyDataPoint>> baselineData;

    public SimulatorFacade(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.dataEngine = new SyntheticDataEngine(42L);
        this.forecaster = new HoltWintersForecaster();
        this.scenarioEngine = new ScenarioEngine();
    }

    public synchronized Map<String, List<HourlyDataPoint>> getBaselineData() {
        if (baselineData == null) {
            baselineData = dataEngine.generateDefaultHospital();
            log.info("Generated baseline data: {} wards, {} hours each",
                    baselineData.size(), baselineData.values().iterator().next().size());
        }
        return baselineData;
    }

    /**
     * Run a scenario simulation for all wards.
     */
    public Map<String, ScenarioResult> runScenario(ScenarioParams params) {
        Map<String, List<HourlyDataPoint>> baseline = getBaselineData();
        Map<String, ScenarioResult> results = new LinkedHashMap<>();

        for (var entry : baseline.entrySet()) {
            ScenarioResult result = scenarioEngine.run(entry.getValue(), params);
            results.put(entry.getKey(), result);

            // Emit SurgePredicted event if peak occupancy is concerning
            if (result.kpis().peakOccupancyPct() > 85) {
                emitSurgeEvent(entry.getKey(), result);
            }
        }

        log.info("Scenario '{}' completed for {} wards", params.scenarioName(), results.size());
        return results;
    }

    /**
     * Get a 72h forecast for a specific ward.
     */
    public HoltWintersForecaster.ForecastResult getForecast(String wardName) {
        Map<String, List<HourlyDataPoint>> baseline = getBaselineData();
        List<HourlyDataPoint> wardData = baseline.get(wardName);
        if (wardData == null) throw new IllegalArgumentException("Ward not found: " + wardName);

        double[] admissions = wardData.stream().mapToDouble(HourlyDataPoint::admissions).toArray();
        return forecaster.forecast(admissions);
    }

    /**
     * Run backtest for a ward and return MAPE report.
     */
    public HoltWintersForecaster.BacktestReport runBacktest(String wardName) {
        Map<String, List<HourlyDataPoint>> baseline = getBaselineData();
        List<HourlyDataPoint> wardData = baseline.get(wardName);
        if (wardData == null) throw new IllegalArgumentException("Ward not found: " + wardName);

        double[] admissions = wardData.stream().mapToDouble(HourlyDataPoint::admissions).toArray();
        return forecaster.backtest(admissions, 0.8);
    }

    /**
     * Compute depletion hours for each resource type for a ward.
     */
    public Map<String, Integer> getDepletionEstimates(String wardName) {
        List<HourlyDataPoint> wardData = getBaselineData().get(wardName);
        if (wardData == null) throw new IllegalArgumentException("Ward not found: " + wardName);

        HourlyDataPoint latest = wardData.get(wardData.size() - 1);
        double[] admForecast = getForecast(wardName).forecasts();

        Map<String, Integer> depletions = new LinkedHashMap<>();

        // Beds: each admission consumes ~1 bed
        depletions.put("beds", forecaster.hourOfDepletion(latest.availableBeds(), admForecast));

        // ICU: ~15% of admissions need ICU
        double[] icuConsumption = Arrays.stream(admForecast).map(a -> a * 0.15).toArray();
        depletions.put("icu", forecaster.hourOfDepletion(latest.availableIcu(), icuConsumption));

        // Ventilators: ~8% of admissions need ventilators
        double[] ventConsumption = Arrays.stream(admForecast).map(a -> a * 0.08).toArray();
        depletions.put("ventilators", forecaster.hourOfDepletion(
                latest.totalVentilators() - latest.usedVentilators(), ventConsumption));

        return depletions;
    }

    /**
     * Get list of available ward names.
     */
    public Set<String> getWardNames() {
        return getBaselineData().keySet();
    }

    /**
     * Get preset scenario params by name.
     */
    public ScenarioParams getPreset(String presetName) {
        return switch (presetName.toLowerCase()) {
            case "flood", "monsoon" -> ScenarioEngine.monsoonFloodPreset();
            case "dengue" -> ScenarioEngine.dengueOutbreakPreset();
            case "pileup", "highway" -> ScenarioEngine.highwayPileUpPreset();
            default -> ScenarioEngine.normalPreset();
        };
    }

    private void emitSurgeEvent(String wardName, ScenarioResult result) {
        // Find most critical resource
        String critical = "beds";
        Map<String, Integer> depletions = getDepletionEstimates(wardName);
        int minHours = Integer.MAX_VALUE;
        for (var e : depletions.entrySet()) {
            if (e.getValue() >= 0 && e.getValue() < minHours) {
                minHours = e.getValue();
                critical = e.getKey();
            }
        }

        SurgePredicted event = new SurgePredicted(
                wardName,
                result.params().scenarioName(),
                result.kpis().peakOccupancyPct(),
                minHours == Integer.MAX_VALUE ? -1 : minHours,
                critical,
                result.kpis().patientsTurnedAway()
        );

        eventPublisher.publishEvent(event);
        log.info("SurgePredicted event emitted for ward '{}': peak={}%, depletion={}h ({})",
                wardName, String.format("%.1f", event.peakOccupancyPct()),
                event.hoursUntilDepletion(), event.depletingResource());
    }
}
