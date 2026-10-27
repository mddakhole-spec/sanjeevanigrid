package com.sanjeevanigrid.simulator.domain;

/**
 * Describes a surge event injected into synthetic data.
 */
public record SurgeEventDescriptor(
        int startHour,
        int durationHours,
        double peakMultiplier,
        String description
) {}
