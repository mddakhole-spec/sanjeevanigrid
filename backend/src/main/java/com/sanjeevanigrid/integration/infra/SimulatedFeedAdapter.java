package com.sanjeevanigrid.integration.infra;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import com.sanjeevanigrid.common.event.StockLow;
import com.sanjeevanigrid.integration.domain.HospitalFeedGateway;
import com.sanjeevanigrid.integration.domain.HospitalFeedLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class SimulatedFeedAdapter implements HospitalFeedGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedFeedAdapter.class);

    private final BloodStockRepository stockRepo;
    private final HospitalFeedLogRepository feedLogRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    public SimulatedFeedAdapter(BloodStockRepository stockRepo,
                                HospitalFeedLogRepository feedLogRepo,
                                ApplicationEventPublisher eventPublisher) {
        this.stockRepo = stockRepo;
        this.feedLogRepo = feedLogRepo;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public FeedProcessResult processFeed(UUID hospitalId, String sourceType, String rawPayload) {
        return triggerSimulatedUpdate(hospitalId);
    }

    public FeedProcessResult triggerSimulatedUpdate(UUID hospitalId) {
        List<BloodStock> stocks = stockRepo.findByHospitalId(hospitalId);
        List<String> actions = new ArrayList<>();
        int changesCount = 0;

        if (!stocks.isEmpty()) {
            // Pick 2 random blood stocks to adjust
            int countToTweak = Math.min(3, stocks.size());
            Collections.shuffle(stocks);

            for (int i = 0; i < countToTweak; i++) {
                BloodStock s = stocks.get(i);
                // 50% chance of consumption (-1 to -2), 50% chance of replenishment (+1 to +3)
                int delta = random.nextBoolean() ? -(random.nextInt(2) + 1) : (random.nextInt(3) + 1);
                int oldUnits = s.getUnitsAvailable();
                int newUnits = Math.max(0, oldUnits + delta);
                s.setUnitsAvailable(newUnits);
                stockRepo.save(s);
                changesCount++;

                actions.add(String.format("Adjusted %s from %d to %d units (%+d)", s.getBloodGroup().getLabel(), oldUnits, newUnits, delta));

                if (s.isLowStock()) {
                    log.warn("[SIMULATED_FEED] Simulated low stock trigger for {} at hospital {}", s.getBloodGroup(), hospitalId);
                    eventPublisher.publishEvent(new StockLow(hospitalId, "Hospital", s.getBloodGroup().name(), s.getUnitsAvailable(), s.getLowStockThreshold()));
                }
            }
        }

        // Simulate oxygen and bed updates
        double oxygenPct = 95.0 + (random.nextDouble() * 4.9);
        actions.add(String.format("Recorded telemetry: Ward oxygen level at %.1f%%", oxygenPct));

        String summary = "Simulated EHR feed cycle: " + String.join("; ", actions);
        String payloadJson = String.format("{\"simulation\":true,\"timestamp\":\"%s\",\"actions\":%s}",
                java.time.Instant.now(), actions.size());

        HospitalFeedLog feedLog = new HospitalFeedLog(
                hospitalId,
                "SIMULATED",
                "Observation",
                "SUCCESS",
                summary,
                payloadJson
        );
        feedLogRepo.save(feedLog);

        log.info("[SIMULATED_FEED] Completed feed cycle for hospital {}: {}", hospitalId, summary);

        return new FeedProcessResult(
                true,
                "Observation",
                summary,
                changesCount,
                "{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"severity\":\"information\",\"diagnostics\":\"" + summary.replace("\"", "'") + "\"}]}"
        );
    }

    @Override
    public String getGatewayType() {
        return "SIMULATED_FEED";
    }
}
