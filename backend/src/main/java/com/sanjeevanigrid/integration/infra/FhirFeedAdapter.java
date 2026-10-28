package com.sanjeevanigrid.integration.infra;

import com.sanjeevanigrid.integration.domain.FhirPayloadParser;
import com.sanjeevanigrid.integration.domain.HospitalFeedGateway;
import com.sanjeevanigrid.integration.domain.HospitalFeedLog;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FhirFeedAdapter implements HospitalFeedGateway {

    private final FhirPayloadParser fhirParser;
    private final HospitalFeedLogRepository feedLogRepo;

    public FhirFeedAdapter(FhirPayloadParser fhirParser, HospitalFeedLogRepository feedLogRepo) {
        this.fhirParser = fhirParser;
        this.feedLogRepo = feedLogRepo;
    }

    @Override
    public FeedProcessResult processFeed(UUID hospitalId, String sourceType, String rawPayload) {
        FhirPayloadParser.ParsedFhirSummary summary = fhirParser.parseAndApply(hospitalId, rawPayload);

        HospitalFeedLog log = new HospitalFeedLog(
                hospitalId,
                sourceType,
                summary.primaryResourceType(),
                summary.success() ? "SUCCESS" : "ERROR",
                String.join("; ", summary.actionsTaken()),
                rawPayload
        );
        feedLogRepo.save(log);

        return new FeedProcessResult(
                summary.success(),
                summary.primaryResourceType(),
                String.join("; ", summary.actionsTaken()),
                summary.entriesProcessed(),
                summary.operationOutcomeJson()
        );
    }

    @Override
    public String getGatewayType() {
        return "FHIR_FEED";
    }
}
