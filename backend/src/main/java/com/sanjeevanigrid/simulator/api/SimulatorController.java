package com.sanjeevanigrid.simulator.api;

import com.sanjeevanigrid.simulator.domain.*;
import com.sanjeevanigrid.simulator.facade.SimulatorFacade;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * REST API for the Crisis Resource Simulator.
 */
@RestController
@RequestMapping("/api/sim")
@CrossOrigin(origins = "*")
public class SimulatorController {

    private final SimulatorFacade facade;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SimulatorController(SimulatorFacade facade) {
        this.facade = facade;
    }

    /**
     * POST /api/sim/run – Run a crisis scenario simulation.
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, ScenarioResult>> runScenario(@RequestBody ScenarioParams params) {
        Map<String, ScenarioResult> results = facade.runScenario(params);

        // Broadcast scenario results to SSE subscribers
        broadcastSse("scenario-result", results);

        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/sim/presets – List available preset scenarios.
     */
    @GetMapping("/presets")
    public ResponseEntity<Map<String, ScenarioParams>> getPresets() {
        Map<String, ScenarioParams> presets = new LinkedHashMap<>();
        presets.put("flood", ScenarioEngine.monsoonFloodPreset());
        presets.put("dengue", ScenarioEngine.dengueOutbreakPreset());
        presets.put("pileup", ScenarioEngine.highwayPileUpPreset());
        presets.put("normal", ScenarioEngine.normalPreset());
        return ResponseEntity.ok(presets);
    }

    /**
     * GET /api/sim/preset/{name} – Run a specific preset.
     */
    @GetMapping("/preset/{name}")
    public ResponseEntity<Map<String, ScenarioResult>> runPreset(@PathVariable String name) {
        ScenarioParams params = facade.getPreset(name);
        Map<String, ScenarioResult> results = facade.runScenario(params);
        broadcastSse("scenario-result", results);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/sim/baseline – Get baseline data for all wards.
     */
    @GetMapping("/baseline")
    public ResponseEntity<Map<String, List<HourlyDataPoint>>> getBaseline() {
        return ResponseEntity.ok(facade.getBaselineData());
    }

    /**
     * GET /api/sim/baseline/{ward} – Get baseline data for a specific ward (last N hours).
     */
    @GetMapping("/baseline/{ward}")
    public ResponseEntity<List<HourlyDataPoint>> getWardBaseline(
            @PathVariable String ward,
            @RequestParam(defaultValue = "72") int hours) {
        Map<String, List<HourlyDataPoint>> baseline = facade.getBaselineData();
        List<HourlyDataPoint> wardData = baseline.get(ward);
        if (wardData == null) return ResponseEntity.notFound().build();

        int start = Math.max(0, wardData.size() - hours);
        return ResponseEntity.ok(wardData.subList(start, wardData.size()));
    }

    /**
     * GET /api/sim/forecast/{ward} – 72h forecast for a ward.
     */
    @GetMapping("/forecast/{ward}")
    public ResponseEntity<HoltWintersForecaster.ForecastResult> getForecast(@PathVariable String ward) {
        try {
            return ResponseEntity.ok(facade.getForecast(ward));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/sim/backtest/{ward} – Backtest MAPE report for a ward's forecaster.
     */
    @GetMapping("/backtest/{ward}")
    public ResponseEntity<HoltWintersForecaster.BacktestReport> getBacktest(@PathVariable String ward) {
        try {
            return ResponseEntity.ok(facade.runBacktest(ward));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/sim/depletion/{ward} – Resource depletion estimates.
     */
    @GetMapping("/depletion/{ward}")
    public ResponseEntity<Map<String, Integer>> getDepletion(@PathVariable String ward) {
        try {
            return ResponseEntity.ok(facade.getDepletionEstimates(ward));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/sim/wards – List available ward names.
     */
    @GetMapping("/wards")
    public ResponseEntity<Set<String>> getWards() {
        return ResponseEntity.ok(facade.getWardNames());
    }

    /**
     * GET /api/sim/compare – Run two scenarios side by side.
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareScenarios(@RequestBody CompareRequest request) {
        Map<String, ScenarioResult> baseline = facade.runScenario(request.baseline());
        Map<String, ScenarioResult> alternative = facade.runScenario(request.alternative());

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("baseline", baseline);
        comparison.put("alternative", alternative);

        // Compute improvement deltas per ward
        Map<String, Map<String, Object>> deltas = new LinkedHashMap<>();
        for (String ward : baseline.keySet()) {
            ScenarioKPIs bKpi = baseline.get(ward).kpis();
            ScenarioKPIs aKpi = alternative.get(ward).kpis();
            Map<String, Object> delta = new LinkedHashMap<>();
            delta.put("turnedAwayDelta", bKpi.patientsTurnedAway() - aKpi.patientsTurnedAway());
            delta.put("peakOccupancyDelta", bKpi.peakOccupancyPct() - aKpi.peakOccupancyPct());
            delta.put("hoursAbove90Delta", bKpi.hoursAbove90Pct() - aKpi.hoursAbove90Pct());
            deltas.put(ward, delta);
        }
        comparison.put("deltas", deltas);

        return ResponseEntity.ok(comparison);
    }

    /**
     * GET /api/sim/stream – Server-Sent Events stream for live dashboard updates.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send initial heartbeat
        try {
            emitter.send(SseEmitter.event().name("connected").data("Simulator SSE connected"));
        } catch (IOException ignored) {}

        return emitter;
    }

    private void broadcastSse(String eventName, Object data) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    /**
     * Request body for comparing two scenarios.
     */
    public record CompareRequest(ScenarioParams baseline, ScenarioParams alternative) {}
}
