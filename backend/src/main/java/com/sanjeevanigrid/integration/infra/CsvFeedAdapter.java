package com.sanjeevanigrid.integration.infra;

import com.sanjeevanigrid.integration.domain.CsvValidationEngine;
import com.sanjeevanigrid.integration.domain.HospitalFeedGateway;
import com.sanjeevanigrid.integration.domain.HospitalFeedLog;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class CsvFeedAdapter implements HospitalFeedGateway {

    private final CsvValidationEngine csvEngine;
    private final HospitalFeedLogRepository feedLogRepo;

    public CsvFeedAdapter(CsvValidationEngine csvEngine, HospitalFeedLogRepository feedLogRepo) {
        this.csvEngine = csvEngine;
        this.feedLogRepo = feedLogRepo;
    }

    @Override
    public FeedProcessResult processFeed(UUID hospitalId, String sourceType, String rawPayload) {
        int committed = csvEngine.commitCsv(hospitalId, "BLOOD_STOCK", rawPayload, Map.of());

        String summary = "Committed " + committed + " records from CSV batch.";
        HospitalFeedLog log = new HospitalFeedLog(
                hospitalId,
                sourceType,
                "CSV_BATCH",
                "SUCCESS",
                summary,
                rawPayload
        );
        feedLogRepo.save(log);

        return new FeedProcessResult(
                true,
                "CSV_BATCH",
                summary,
                committed,
                "{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"severity\":\"information\",\"diagnostics\":\"" + summary + "\"}]}"
        );
    }

    @Override
    public String getGatewayType() {
        return "CSV_FEED";
    }
}
