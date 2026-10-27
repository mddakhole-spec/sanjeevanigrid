package com.sanjeevanigrid.common.event;

import java.time.Instant;
import java.util.Map;

/**
 * Domain event emitted when the simulator forecasts a surge.
 * Published via Spring ApplicationEventPublisher.
 * Design note: can later be swapped for Kafka.
 */
public record SurgePredicted(
        String wardName,
        String scenarioName,
        double peakOccupancyPct,
        int hoursUntilDepletion,
        String depletingResource,
        int patientsTurnedAway,
        Map<String, Object> metadata,
        Instant timestamp
) {
    public SurgePredicted(String wardName, String scenarioName,
                          double peakOccupancyPct, int hoursUntilDepletion,
                          String depletingResource, int patientsTurnedAway) {
        this(wardName, scenarioName, peakOccupancyPct, hoursUntilDepletion,
                depletingResource, patientsTurnedAway, Map.of(), Instant.now());
    }
}
