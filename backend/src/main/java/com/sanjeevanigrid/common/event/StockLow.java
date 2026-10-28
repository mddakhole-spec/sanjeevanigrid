package com.sanjeevanigrid.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when blood inventory reaches or drops below low-stock threshold.
 * Published via Spring ApplicationEventPublisher.
 */
public record StockLow(
        UUID hospitalId,
        String hospitalName,
        String bloodGroup,
        int unitsRemaining,
        int threshold,
        Instant timestamp
) {
    public StockLow(UUID hospitalId, String hospitalName, String bloodGroup, int unitsRemaining, int threshold) {
        this(hospitalId, hospitalName, bloodGroup, unitsRemaining, threshold, Instant.now());
    }
}
