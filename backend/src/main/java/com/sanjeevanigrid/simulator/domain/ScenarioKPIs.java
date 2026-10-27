package com.sanjeevanigrid.simulator.domain;

/**
 * KPIs computed from a scenario simulation.
 */
public record ScenarioKPIs(
        int patientsTurnedAway,
        double peakOccupancyPct,
        int hoursAbove90Pct,
        int totalHours
) {}
