package com.sanjeevanigrid.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a medical camp is marked as completed.
 * Enables the equity module to update recent camp coverage and recalculate village access scores.
 */
public record CampCompleted(
        UUID campId,
        UUID villageId,
        String villageName,
        int attendeesCount,
        Instant completedAt
) {
    public CampCompleted(UUID campId, UUID villageId, String villageName, int attendeesCount) {
        this(campId, villageId, villageName, attendeesCount, Instant.now());
    }
}

