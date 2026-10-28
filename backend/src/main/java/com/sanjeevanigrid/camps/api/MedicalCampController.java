package com.sanjeevanigrid.camps.api;

import com.sanjeevanigrid.camps.domain.*;
import com.sanjeevanigrid.camps.facade.CampFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/camps")
@Tag(name = "Medical Camps & Offline PWA", description = "Medical camp creation, offline idempotent sync, vaccine engine, and reports")
public class MedicalCampController {

    private final CampFacade campFacade;

    public MedicalCampController(CampFacade campFacade) {
        this.campFacade = campFacade;
    }

    public record CreateCampRequest(
            String name,
            String organizerName,
            String campType,
            String venue,
            UUID villageId,
            String villageName,
            String district,
            Double latitude,
            Double longitude,
            LocalDate campDate,
            LocalTime startTime,
            LocalTime endTime,
            String doctorInCharge,
            int targetAttendees
    ) {}

    public record VaccineScheduleRequest(
            LocalDate dob,
            List<VaccineScheduleEngine.AdministeredDose> history
    ) {}

    public record DispenseRequest(
            String medicineName,
            int quantity
    ) {}

    @GetMapping
    @Operation(summary = "List all medical camps")
    public ResponseEntity<List<MedicalCamp>> getAllCamps() {
        return ResponseEntity.ok(campFacade.getAllCamps());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get camp by ID")
    public ResponseEntity<MedicalCamp> getCamp(@PathVariable UUID id) {
        return campFacade.getCampById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new medical camp (can be pre-filled from equity recommendations)")
    public ResponseEntity<MedicalCamp> createCamp(@RequestBody CreateCampRequest req) {
        MedicalCamp camp = new MedicalCamp(
                req.name(),
                req.organizerName(),
                req.campType(),
                req.venue(),
                req.villageId(),
                req.villageName(),
                req.district(),
                req.latitude(),
                req.longitude(),
                req.campDate(),
                req.startTime(),
                req.endTime(),
                req.doctorInCharge(),
                req.targetAttendees()
        );
        return ResponseEntity.ok(campFacade.createCamp(camp));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update camp status (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)")
    public ResponseEntity<MedicalCamp> updateCampStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "IN_PROGRESS");
        return ResponseEntity.ok(campFacade.updateCampStatus(id, status));
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Idempotent batch synchronization for offline PWA (client UUIDs, registrations, services, vaccines, stock)")
    public ResponseEntity<CampSyncDto.SyncResult> syncBatch(
            @PathVariable UUID id,
            @RequestBody CampSyncDto.SyncBatch batch) {
        return ResponseEntity.ok(campFacade.syncOfflineBatch(id, batch));
    }

    @GetMapping("/{id}/attendees")
    @Operation(summary = "List registered attendees for camp")
    public ResponseEntity<List<CampAttendee>> getCampAttendees(@PathVariable UUID id) {
        return ResponseEntity.ok(campFacade.getCampAttendees(id));
    }

    @PostMapping("/{id}/attendees")
    @Operation(summary = "Register single attendee (calls idempotent sync)")
    public ResponseEntity<CampSyncDto.SyncResult> registerSingleAttendee(
            @PathVariable UUID id,
            @RequestBody CampSyncDto.SyncRegistration reg) {
        CampSyncDto.SyncBatch batch = new CampSyncDto.SyncBatch(
                id,
                List.of(reg),
                List.of(),
                List.of(),
                List.of()
        );
        return ResponseEntity.ok(campFacade.syncOfflineBatch(id, batch));
    }

    @GetMapping("/{id}/medicine-stock")
    @Operation(summary = "Get camp medicine inventory")
    public ResponseEntity<List<CampMedicineStock>> getMedicineStock(@PathVariable UUID id) {
        return ResponseEntity.ok(campFacade.getCampMedicineStock(id));
    }

    @PostMapping("/{id}/medicine-stock/dispense")
    @Operation(summary = "Dispense medicine from camp stock")
    public ResponseEntity<Map<String, Object>> dispenseMedicine(
            @PathVariable UUID id,
            @RequestBody DispenseRequest req) {
        boolean ok = campFacade.dispenseMedicine(id, req.medicineName(), req.quantity());
        return ResponseEntity.ok(Map.of("success", ok, "medicine", req.medicineName(), "quantity", req.quantity()));
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Generate structured camp report with triage, vitals alerts, demographics, and vaccines")
    public ResponseEntity<CampReportGenerator.CampReportDto> getCampReport(@PathVariable UUID id) {
        return ResponseEntity.ok(campFacade.getCampReport(id));
    }

    @GetMapping("/{id}/report/csv")
    @Operation(summary = "Export full camp attendee log as CSV")
    public ResponseEntity<String> getCampReportCsv(@PathVariable UUID id) {
        String csv = campFacade.getCampCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"camp-" + id + "-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @PostMapping("/vaccine-schedule")
    @Operation(summary = "Evaluate UIP child immunization schedule based on DOB and past history")
    public ResponseEntity<VaccineScheduleEngine.ScheduleResult> evaluateVaccineSchedule(
            @RequestBody VaccineScheduleRequest req) {
        return ResponseEntity.ok(campFacade.evaluateVaccineSchedule(req.dob(), req.history()));
    }
}

