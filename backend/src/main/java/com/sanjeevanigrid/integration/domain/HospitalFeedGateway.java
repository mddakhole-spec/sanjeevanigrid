package com.sanjeevanigrid.integration.domain;

import java.util.UUID;

/**
 * Hospital feed gateway interface (adapter pattern).
 * Implementations: FhirFeedAdapter, CsvFeedAdapter, SimulatedFeedAdapter.
 */
public interface HospitalFeedGateway {

    record FeedProcessResult(
            boolean success,
            String resourceType,
            String summary,
            int recordsUpdated,
            String operationOutcome
    ) {}

    FeedProcessResult processFeed(UUID hospitalId, String sourceType, String rawPayload);

    String getGatewayType();
}
