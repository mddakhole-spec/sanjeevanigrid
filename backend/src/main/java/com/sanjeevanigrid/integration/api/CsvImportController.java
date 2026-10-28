package com.sanjeevanigrid.integration.api;

import com.sanjeevanigrid.integration.domain.CsvValidationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integration/csv")
public class CsvImportController {

    private static final UUID DEFAULT_HOSPITAL_ID = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    private final CsvValidationEngine csvEngine;

    public CsvImportController(CsvValidationEngine csvEngine) {
        this.csvEngine = csvEngine;
    }

    @PostMapping("/preview")
    public ResponseEntity<CsvValidationEngine.CsvPreviewResult> previewCsv(@RequestBody Map<String, Object> body) {
        String entityType = body.getOrDefault("entityType", "BLOOD_STOCK").toString();
        String csvContent = body.getOrDefault("csvContent", "").toString();

        @SuppressWarnings("unchecked")
        Map<String, String> customMapping = body.containsKey("columnMapping")
                ? (Map<String, String>) body.get("columnMapping")
                : Map.of();

        return ResponseEntity.ok(csvEngine.previewCsv(entityType, csvContent, customMapping));
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitCsv(@RequestBody Map<String, Object> body) {
        UUID hospitalId = body.containsKey("hospitalId")
                ? UUID.fromString(body.get("hospitalId").toString())
                : DEFAULT_HOSPITAL_ID;

        String entityType = body.getOrDefault("entityType", "BLOOD_STOCK").toString();
        String csvContent = body.getOrDefault("csvContent", "").toString();

        @SuppressWarnings("unchecked")
        Map<String, String> customMapping = body.containsKey("columnMapping")
                ? (Map<String, String>) body.get("columnMapping")
                : Map.of();

        int committed = csvEngine.commitCsv(hospitalId, entityType, csvContent, customMapping);

        return ResponseEntity.ok(Map.of(
                "status", "COMMITTED",
                "entityType", entityType,
                "recordsImported", committed,
                "message", String.format("Successfully committed %d %s record(s) into database.", committed, entityType)
        ));
    }
}
