package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.*;
import com.sanjeevanigrid.blood.infra.BloodDonorRepository;
import com.sanjeevanigrid.blood.infra.PostGisDonorGeoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/blood/donors")
public class BloodDonorController {

    private final BloodDonorRepository donorRepo;
    private final PostGisDonorGeoService geoService;
    private final EligibilityCalculator eligibilityCalculator;
    private final ReliabilityScorer reliabilityScorer;

    public BloodDonorController(BloodDonorRepository donorRepo,
                                PostGisDonorGeoService geoService,
                                EligibilityCalculator eligibilityCalculator,
                                ReliabilityScorer reliabilityScorer) {
        this.donorRepo = donorRepo;
        this.geoService = geoService;
        this.eligibilityCalculator = eligibilityCalculator;
        this.reliabilityScorer = reliabilityScorer;
    }

    @PostMapping("/register")
    public ResponseEntity<BloodDonor> registerDonor(@Valid @RequestBody RegisterDonorDto dto) {
        BloodDonor donor = new BloodDonor(
                dto.fullName(),
                dto.phone(),
                dto.bloodGroup(),
                dto.latitude(),
                dto.longitude(),
                dto.address(),
                dto.district()
        );
        donor.setEmail(dto.email());
        donor.setConsentGiven(dto.consentGiven());
        return ResponseEntity.ok(donorRepo.save(donor));
    }

    @GetMapping
    public ResponseEntity<List<BloodDonor>> getDonors(
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) String district) {
        if (bloodGroup != null) {
            return ResponseEntity.ok(donorRepo.findByBloodGroup(bloodGroup));
        }
        if (district != null && !district.isBlank()) {
            return ResponseEntity.ok(donorRepo.findByDistrict(district));
        }
        return ResponseEntity.ok(donorRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BloodDonor> getDonorById(@PathVariable UUID id) {
        return ResponseEntity.of(donorRepo.findById(id));
    }

    @PatchMapping("/{id}/toggle-availability")
    public ResponseEntity<BloodDonor> toggleAvailability(@PathVariable UUID id) {
        BloodDonor donor = donorRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Donor not found: " + id));
        donor.setAvailable(!donor.isAvailable());
        return ResponseEntity.ok(donorRepo.save(donor));
    }

    @PatchMapping("/{id}/toggle-consent")
    public ResponseEntity<BloodDonor> toggleConsent(@PathVariable UUID id) {
        BloodDonor donor = donorRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Donor not found: " + id));
        donor.setConsentGiven(!donor.isConsentGiven());
        return ResponseEntity.ok(donorRepo.save(donor));
    }

    @GetMapping("/{id}/eligibility")
    public ResponseEntity<Map<String, Object>> getEligibility(@PathVariable UUID id) {
        BloodDonor donor = donorRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Donor not found: " + id));
        EligibilityCalculator.EligibilityResult eligibility = eligibilityCalculator.checkEligibility(donor);
        ReliabilityScorer.ReliabilityBreakdown reliability = reliabilityScorer.evaluate(donor, java.time.Instant.now());

        return ResponseEntity.ok(Map.of(
                "donorId", donor.getId(),
                "fullName", donor.getFullName(),
                "phone", donor.getPhone(),
                "bloodGroup", donor.getBloodGroup(),
                "isAvailable", donor.isAvailable(),
                "consentGiven", donor.isConsentGiven(),
                "lastDonationDate", donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : "None",
                "eligibility", eligibility,
                "reliability", reliability
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostGisDonorGeoService.RankedDonor>> searchNearbyDonors(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "35.0") double radiusKm,
            @RequestParam BloodGroup bloodGroup) {
        List<BloodGroup> compatible = bloodGroup.getCompatibleDonors();
        return ResponseEntity.ok(geoService.searchAndRankDonors(lat, lon, radiusKm, compatible));
    }
}
