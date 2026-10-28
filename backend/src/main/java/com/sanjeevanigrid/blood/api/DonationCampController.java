package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.CampRsvp;
import com.sanjeevanigrid.blood.domain.DonationCamp;
import com.sanjeevanigrid.blood.infra.CampRsvpRepository;
import com.sanjeevanigrid.blood.infra.DonationCampRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/blood/camps")
public class DonationCampController {

    private final DonationCampRepository campRepo;
    private final CampRsvpRepository rsvpRepo;

    public DonationCampController(DonationCampRepository campRepo, CampRsvpRepository rsvpRepo) {
        this.campRepo = campRepo;
        this.rsvpRepo = rsvpRepo;
    }

    @GetMapping
    public ResponseEntity<List<DonationCamp>> getUpcomingCamps(@RequestParam(required = false) String district) {
        if (district != null && !district.isBlank()) {
            return ResponseEntity.ok(campRepo.findByDistrictOrderByCampDateAsc(district));
        }
        return ResponseEntity.ok(campRepo.findByCampDateGreaterThanEqualOrderByCampDateAsc(LocalDate.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationCamp> getCampById(@PathVariable UUID id) {
        return ResponseEntity.of(campRepo.findById(id));
    }

    @PostMapping
    public ResponseEntity<DonationCamp> createCamp(@RequestBody DonationCamp camp) {
        if (camp.getCampDate() == null) {
            camp.setCampDate(LocalDate.now().plusDays(7));
        }
        return ResponseEntity.ok(campRepo.save(camp));
    }

    @PostMapping("/{id}/rsvp")
    public ResponseEntity<Map<String, Object>> rsvpCamp(@PathVariable UUID id, @Valid @RequestBody CampRsvpRequestDto dto) {
        DonationCamp camp = campRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Camp not found: " + id));

        // Check if already registered
        if (rsvpRepo.findByCampIdAndDonorPhone(id, dto.donorPhone()).isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "status", "ALREADY_REGISTERED",
                    "message", "Donor " + dto.donorPhone() + " is already registered for this camp.",
                    "registeredCount", camp.getRegisteredCount()
            ));
        }

        CampRsvp rsvp = new CampRsvp(id, dto.donorName(), dto.donorPhone(), dto.bloodGroup(), dto.notes());
        rsvpRepo.save(rsvp);

        camp.incrementRsvp();
        campRepo.save(camp);

        return ResponseEntity.ok(Map.of(
                "status", "REGISTERED",
                "message", "RSVP confirmed for " + dto.donorName() + " (" + dto.bloodGroup().getLabel() + ")!",
                "registeredCount", camp.getRegisteredCount()
        ));
    }

    @GetMapping("/{id}/rsvps")
    public ResponseEntity<List<CampRsvp>> getCampRsvps(@PathVariable UUID id) {
        return ResponseEntity.ok(rsvpRepo.findByCampIdOrderByCreatedAtAsc(id));
    }
}
