package com.sanjeevanigrid.integration.api;

import com.sanjeevanigrid.integration.domain.HospitalFeedGateway;
import com.sanjeevanigrid.integration.domain.HospitalFeedLog;
import com.sanjeevanigrid.integration.infra.HospitalFeedLogRepository;
import com.sanjeevanigrid.integration.infra.SimulatedFeedAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integration")
public class SimulatedFeedController {

    private static final UUID DEFAULT_HOSPITAL_ID = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    private final SimulatedFeedAdapter simulatedFeedAdapter;
    private final HospitalFeedLogRepository feedLogRepo;

    public SimulatedFeedController(SimulatedFeedAdapter simulatedFeedAdapter,
                                   HospitalFeedLogRepository feedLogRepo) {
        this.simulatedFeedAdapter = simulatedFeedAdapter;
        this.feedLogRepo = feedLogRepo;
    }

    @PostMapping("/simulate-feed")
    public ResponseEntity<HospitalFeedGateway.FeedProcessResult> triggerSimulatedFeed(
            @RequestBody(required = false) Map<String, String> body) {
        UUID hospitalId = DEFAULT_HOSPITAL_ID;
        if (body != null && body.containsKey("hospitalId")) {
            hospitalId = UUID.fromString(body.get("hospitalId"));
        }

        HospitalFeedGateway.FeedProcessResult result = simulatedFeedAdapter.triggerSimulatedUpdate(hospitalId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/feed-logs")
    public ResponseEntity<List<HospitalFeedLog>> getFeedLogs(@RequestParam(required = false) UUID hospitalId) {
        if (hospitalId != null) {
            return ResponseEntity.ok(feedLogRepo.findByHospitalIdOrderByCreatedAtDesc(hospitalId));
        }
        return ResponseEntity.ok(feedLogRepo.findAllByOrderByCreatedAtDesc());
    }
}
