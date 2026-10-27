package com.sanjeevanigrid.simulator.domain;

import java.time.LocalDateTime;

/**
 * A single hourly data point for a ward.
 */
public record HourlyDataPoint(
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
        double oxygenLevelPct
) {
    public double bedOccupancyPct() {
        return totalBeds == 0 ? 0 : (double) occupiedBeds / totalBeds * 100.0;
    }

    public double icuOccupancyPct() {
        return totalIcu == 0 ? 0 : (double) occupiedIcu / totalIcu * 100.0;
    }

    public double ventilatorUsagePct() {
        return totalVentilators == 0 ? 0 : (double) usedVentilators / totalVentilators * 100.0;
    }

    public int availableBeds() {
        return totalBeds - occupiedBeds;
    }

    public int availableIcu() {
        return totalIcu - occupiedIcu;
    }
}
