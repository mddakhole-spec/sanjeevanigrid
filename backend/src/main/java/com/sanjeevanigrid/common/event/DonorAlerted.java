package com.sanjeevanigrid.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a blood donor has been contacted via alert adapter.
 */
public record DonorAlerted(
        UUID requestId,
        UUID donorId,
        String donorName,
        String donorPhone,
        String bloodGroup,
        String channel,
        double reliabilityScore,
        double distanceKm,
        Instant timestamp
) {
    public DonorAlerted(UUID requestId, UUID donorId, String donorName, String donorPhone, String bloodGroup, String channel, double reliabilityScore, double distanceKm) {
        this(requestId, donorId, donorName, donorPhone, bloodGroup, channel, reliabilityScore, distanceKm, Instant.now());
    }
}
