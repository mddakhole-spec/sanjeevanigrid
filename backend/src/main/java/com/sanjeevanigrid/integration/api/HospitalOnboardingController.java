package com.sanjeevanigrid.integration.api;

import com.sanjeevanigrid.integration.domain.ApiKeyService;
import com.sanjeevanigrid.integration.domain.Hospital;
import com.sanjeevanigrid.integration.infra.HospitalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integration/hospitals")
public class HospitalOnboardingController {

    private final HospitalRepository hospitalRepo;
    private final ApiKeyService apiKeyService;

    public HospitalOnboardingController(HospitalRepository hospitalRepo, ApiKeyService apiKeyService) {
        this.hospitalRepo = hospitalRepo;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerHospital(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "New Rural Health Facility");
        String licence = body.getOrDefault("licenceNumber", "LIC-" + System.currentTimeMillis() % 100000);
        String address = body.getOrDefault("address", "District Hospital Road, Yavatmal");
        String district = body.getOrDefault("district", "Yavatmal");
        String phone = body.getOrDefault("phone", "+917232200000");
        String email = body.getOrDefault("email", "info@hospital.in");

        Hospital hospital = new Hospital(name, licence, address, district, phone, email);
        if (body.containsKey("latitude") && body.containsKey("longitude")) {
            hospital.setLatitude(Double.parseDouble(body.get("latitude")));
            hospital.setLongitude(Double.parseDouble(body.get("longitude")));
        }

        Hospital saved = hospitalRepo.save(hospital);

        return ResponseEntity.ok(Map.of(
                "status", "REGISTERED_PENDING_APPROVAL",
                "hospitalId", saved.getId(),
                "name", saved.getName(),
                "licenceNumber", saved.getLicenceNumber(),
                "message", "Hospital onboarding request submitted. Awaiting Platform Admin verification."
        ));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Hospital>> getPendingHospitals() {
        return ResponseEntity.ok(hospitalRepo.findByVerifiedFalse());
    }

    @GetMapping
    public ResponseEntity<List<Hospital>> getVerifiedHospitals() {
        return ResponseEntity.ok(hospitalRepo.findByVerifiedTrue());
    }

    @GetMapping("/all")
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        return ResponseEntity.ok(hospitalRepo.findAll());
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyHospital(@PathVariable UUID id) {
        Hospital hospital = hospitalRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        // Generate cryptographically secure API key
        String rawApiKey = apiKeyService.generateRawApiKey();
        String hashedKey = apiKeyService.hashApiKey(rawApiKey);

        hospital.setVerified(true);
        hospital.setApiKeyHash(hashedKey);
        hospitalRepo.save(hospital);

        return ResponseEntity.ok(Map.of(
                "status", "VERIFIED",
                "hospitalId", hospital.getId(),
                "hospitalName", hospital.getName(),
                "rawApiKey", rawApiKey,
                "message", "Hospital verified successfully! Present this API Key securely to hospital staff. It cannot be recovered later.",
                "feedEndpoint", "/api/feed/" + hospital.getId()
        ));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectHospital(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        Hospital hospital = hospitalRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        String reason = (body != null) ? body.getOrDefault("reason", "Licence document validation failed") : "Invalid credentials";
        hospitalRepo.delete(hospital);

        return ResponseEntity.ok(Map.of(
                "status", "REJECTED",
                "hospitalId", id,
                "reason", reason
        ));
    }
}
