package com.sanjeevanigrid.integration.api;

import com.sanjeevanigrid.integration.domain.ApiKeyService;
import com.sanjeevanigrid.integration.domain.Hospital;
import com.sanjeevanigrid.integration.domain.HospitalFeedGateway;
import com.sanjeevanigrid.integration.infra.FhirFeedAdapter;
import com.sanjeevanigrid.integration.infra.HospitalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
public class HospitalFeedController {

    private final HospitalRepository hospitalRepo;
    private final ApiKeyService apiKeyService;
    private final FhirFeedAdapter fhirAdapter;

    public HospitalFeedController(HospitalRepository hospitalRepo,
                                  ApiKeyService apiKeyService,
                                  FhirFeedAdapter fhirAdapter) {
        this.hospitalRepo = hospitalRepo;
        this.apiKeyService = apiKeyService;
        this.fhirAdapter = fhirAdapter;
    }

    /**
     * FHIR-aligned automated feed endpoint for hospital EHR and telemetry integrations.
     * Authenticated via header 'X-API-KEY' hashed against database records.
     */
    @PostMapping(value = "/{hospitalId}", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> ingestFeed(
            @PathVariable UUID hospitalId,
            @RequestHeader(value = "X-API-KEY", required = false) String rawApiKey,
            @RequestBody String rawPayload) {

        // 1. Verify API Key presence
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("""
                    {
                      "resourceType": "OperationOutcome",
                      "issue": [
                        {
                          "severity": "error",
                          "code": "security",
                          "diagnostics": "Missing required authentication header: X-API-KEY"
                        }
                      ]
                    }
                    """);
        }

        // 2. Fetch hospital
        Optional<Hospital> hospitalOpt = hospitalRepo.findById(hospitalId);
        if (hospitalOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("""
                    {
                      "resourceType": "OperationOutcome",
                      "issue": [
                        {
                          "severity": "error",
                          "code": "not-found",
                          "diagnostics": "Hospital with specified ID does not exist"
                        }
                      ]
                    }
                    """);
        }

        Hospital hospital = hospitalOpt.get();

        // 3. Check verification status
        if (!hospital.isVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("""
                    {
                      "resourceType": "OperationOutcome",
                      "issue": [
                        {
                          "severity": "error",
                          "code": "forbidden",
                          "diagnostics": "Hospital onboarding is pending verification. Feeds are not accepted until verified."
                        }
                      ]
                    }
                    """);
        }

        // 4. Validate API Key hash
        boolean isValidKey = apiKeyService.verifyApiKey(rawApiKey, hospital.getApiKeyHash());
        if (!isValidKey) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("""
                    {
                      "resourceType": "OperationOutcome",
                      "issue": [
                        {
                          "severity": "error",
                          "code": "login",
                          "diagnostics": "Invalid or revoked X-API-KEY"
                        }
                      ]
                    }
                    """);
        }

        // 5. Ingest FHIR payload via gateway adapter
        HospitalFeedGateway.FeedProcessResult result = fhirAdapter.processFeed(hospitalId, "API_KEY", rawPayload);

        return ResponseEntity.ok(result.operationOutcome());
    }
}
