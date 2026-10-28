package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.BloodAllocationService;
import com.sanjeevanigrid.blood.domain.BloodDonorAlert;
import com.sanjeevanigrid.blood.domain.BloodRequest;
import com.sanjeevanigrid.blood.infra.BloodDonorAlertRepository;
import com.sanjeevanigrid.blood.infra.BloodRequestRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/blood/requests")
public class BloodRequestController {

    private final BloodAllocationService allocationService;
    private final BloodRequestRepository requestRepo;
    private final BloodDonorAlertRepository alertRepo;

    public BloodRequestController(BloodAllocationService allocationService,
                                  BloodRequestRepository requestRepo,
                                  BloodDonorAlertRepository alertRepo) {
        this.allocationService = allocationService;
        this.requestRepo = requestRepo;
        this.alertRepo = alertRepo;
    }

    @PostMapping
    public ResponseEntity<BloodAllocationService.AllocationResult> createRequest(@Valid @RequestBody CreateBloodRequestDto dto) {
        BloodAllocationService.AllocationResult result = allocationService.requestBlood(
                dto.hospitalId(),
                dto.patientName(),
                dto.patientPhone(),
                dto.bloodGroup(),
                dto.unitsNeeded(),
                dto.urgency(),
                dto.radiusKm()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<BloodRequest>> getRequests(@RequestParam(required = false) UUID hospitalId) {
        if (hospitalId != null) {
            return ResponseEntity.ok(requestRepo.findByHospitalIdOrderByCreatedAtDesc(hospitalId));
        }
        return ResponseEntity.ok(requestRepo.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRequestDetails(@PathVariable UUID id) {
        BloodRequest req = requestRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Blood request not found: " + id));
        List<BloodDonorAlert> alerts = alertRepo.findByBloodRequestIdOrderByAlertOrderAsc(id);
        return ResponseEntity.ok(Map.of(
                "request", req,
                "alerts", alerts
        ));
    }
}
