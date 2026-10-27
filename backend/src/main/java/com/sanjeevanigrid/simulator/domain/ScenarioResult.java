package com.sanjeevanigrid.simulator.domain;

import java.util.List;

/**
 * Full result of a scenario simulation.
 */
public record ScenarioResult(
        ScenarioParams params,
        List<SimulatedHour> hours,
        ScenarioKPIs kpis,
        List<String> recommendations
) {}
