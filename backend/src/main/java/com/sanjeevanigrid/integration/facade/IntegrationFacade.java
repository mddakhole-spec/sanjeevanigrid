package com.sanjeevanigrid.integration.facade;

import com.sanjeevanigrid.integration.domain.*;
import com.sanjeevanigrid.integration.infra.FhirFeedAdapter;
import com.sanjeevanigrid.integration.infra.HospitalRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public facade for the Integration Hub module.
 * Other modules interact with the Integration module exclusively through this facade.
 */
@Component
public class IntegrationFacade {

    private final HospitalRepository hospitalRepo;
    private final ApiKeyService apiKeyService;
    private final FhirFeedAdapter fhirAdapter;
    private final CsvValidationEngine csvEngine;

    public IntegrationFacade(HospitalRepository hospitalRepo,
                             ApiKeyService apiKeyService,
                             FhirFeedAdapter fhirAdapter,
                             CsvValidationEngine csvEngine) {
        this.hospitalRepo = hospitalRepo;
        this.apiKeyService = apiKeyService;
        this.fhirAdapter = fhirAdapter;
        this.csvEngine = csvEngine;
    }

    public Optional<Hospital> getHospital(UUID hospitalId) {
        return hospitalRepo.findById(hospitalId);
    }

    public Hospital registerHospital(String name, String licence, String address, String district, String phone, String email) {
        Hospital h = new Hospital(name, licence, address, district, phone, email);
        return hospitalRepo.save(h);
    }

    public String verifyHospitalAndGenerateKey(UUID hospitalId) {
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + hospitalId));
        String rawKey = apiKeyService.generateRawApiKey();
        hospital.setVerified(true);
        hospital.setApiKeyHash(apiKeyService.hashApiKey(rawKey));
        hospitalRepo.save(hospital);
        return rawKey;
    }

    public HospitalFeedGateway.FeedProcessResult processFhirFeed(UUID hospitalId, String sourceType, String rawPayload) {
        return fhirAdapter.processFeed(hospitalId, sourceType, rawPayload);
    }

    public CsvValidationEngine.CsvPreviewResult previewCsv(String entityType, String csvContent, Map<String, String> mapping) {
        return csvEngine.previewCsv(entityType, csvContent, mapping);
    }

    public int commitCsv(UUID hospitalId, String entityType, String csvContent, Map<String, String> mapping) {
        return csvEngine.commitCsv(hospitalId, entityType, csvContent, mapping);
    }
}
