package com.sanjeevanigrid.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an emergency or urgent blood request is registered.
 */
public record BloodRequestCreated(
        UUID requestId,
        String requestNumber,
        UUID hospitalId,
        String hospitalName,
        String bloodGroup,
        int unitsNeeded,
        String urgency,
        Instant timestamp
) {
    public BloodRequestCreated(UUID requestId, String requestNumber, UUID hospitalId, String hospitalName, String bloodGroup, int unitsNeeded, String urgency) {
        this(requestId, requestNumber, hospitalId, hospitalName, bloodGroup, unitsNeeded, urgency, Instant.now());
    }
}
