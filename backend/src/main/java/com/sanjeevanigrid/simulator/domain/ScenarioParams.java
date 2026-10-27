package com.sanjeevanigrid.simulator.domain;

/**
 * Parameters for running a crisis scenario simulation.
 */
public record ScenarioParams(
        double surgeMultiplier,
        String triageMode,      // NORMAL, MODERATE, STRICT
        int extraBeds,
        int staffReassignment,
        int durationHours,
        String scenarioName
) {}
