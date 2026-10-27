package com.sanjeevanigrid.simulator.domain;

import java.time.LocalDateTime;

/**
 * A single hour of simulated scenario output.
 */
public record SimulatedHour(
        LocalDateTime timestamp,
        String wardName,
        int admissions,
        int discharges,
        int totalBeds,
        int occupiedBeds,
        int totalIcu,
        int occupiedIcu,
        int totalVentilators,
        int usedVentilators,
        int staffOnDuty,
        double oxygenLevelPct,
        double occupancyPct,
        int turnedAway
) {}
