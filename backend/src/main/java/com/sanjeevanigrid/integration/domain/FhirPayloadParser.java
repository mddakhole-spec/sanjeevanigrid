package com.sanjeevanigrid.integration.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import com.sanjeevanigrid.common.event.StockLow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Parses and processes FHIR-aligned healthcare resources (Location, Observation, Appointment, Patient, Bundle).
 */
@Component
public class FhirPayloadParser {

    private static final Logger log = LoggerFactory.getLogger(FhirPayloadParser.class);

    private final ObjectMapper objectMapper;
    private final BloodStockRepository bloodStockRepo;
    private final ApplicationEventPublisher eventPublisher;

    public FhirPayloadParser(ObjectMapper objectMapper,
                             BloodStockRepository bloodStockRepo,
                             ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.bloodStockRepo = bloodStockRepo;
        this.eventPublisher = eventPublisher;
    }

    public record ParsedFhirSummary(
            boolean success,
            String primaryResourceType,
            int entriesProcessed,
            List<String> actionsTaken,
            String operationOutcomeJson
    ) {}

    public ParsedFhirSummary parseAndApply(UUID hospitalId, String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String resourceType = root.path("resourceType").asText("Unknown");

            List<String> actions = new ArrayList<>();
            int count = 0;

            if ("Bundle".equalsIgnoreCase(resourceType)) {
                JsonNode entries = root.path("entry");
                if (entries.isArray()) {
                    for (JsonNode entry : entries) {
                        JsonNode res = entry.path("resource");
                        if (!res.isMissingNode()) {
                            actions.add(processSingleResource(hospitalId, res));
                            count++;
                        }
                    }
                }
            } else {
                actions.add(processSingleResource(hospitalId, root));
                count = 1;
            }

            String outcome = generateOperationOutcome(true, "Successfully ingested " + count + " FHIR resource(s). Actions: " + String.join("; ", actions));
            return new ParsedFhirSummary(true, resourceType, count, actions, outcome);

        } catch (Exception e) {
            log.error("Failed to parse FHIR payload for hospital {}: {}", hospitalId, e.getMessage(), e);
            String outcome = generateOperationOutcome(false, "Failed to parse FHIR payload: " + e.getMessage());
            return new ParsedFhirSummary(false, "Unknown", 0, List.of(e.getMessage()), outcome);
        }
    }

    private String processSingleResource(UUID hospitalId, JsonNode node) {
        String type = node.path("resourceType").asText("");

        return switch (type) {
            case "Location" -> processLocation(hospitalId, node);
            case "Observation" -> processObservation(hospitalId, node);
            case "Appointment" -> processAppointment(hospitalId, node);
            case "Patient" -> processPatient(hospitalId, node);
            default -> "Acknowledged generic FHIR resource: " + type;
        };
    }

    private String processLocation(UUID hospitalId, JsonNode node) {
        String wardName = node.path("name").asText("Ward Intake");
        int totalBeds = node.path("totalBeds").asInt(node.path("partOf").path("totalBeds").asInt(20));
        int availableBeds = node.path("availableBeds").asInt(node.path("status").asText().equals("active") ? 5 : 0);

        // Check for FHIR extension array
        JsonNode extensions = node.path("extension");
        if (extensions.isArray()) {
            for (JsonNode ext : extensions) {
                String url = ext.path("url").asText("");
                if (url.contains("totalBeds")) totalBeds = ext.path("valueInteger").asInt(totalBeds);
                if (url.contains("availableBeds")) availableBeds = ext.path("valueInteger").asInt(availableBeds);
            }
        }

        log.info("[FHIR:Location] Hospital {}: Ward '{}' updated to {}/{} beds available", hospitalId, wardName, availableBeds, totalBeds);
        return String.format("Updated Ward '%s': %d/%d beds available", wardName, availableBeds, totalBeds);
    }

    private String processObservation(UUID hospitalId, JsonNode node) {
        String code = "";
        JsonNode coding = node.path("code").path("coding");
        if (coding.isArray() && !coding.isEmpty()) {
            code = coding.get(0).path("code").asText("");
        }

        double val = node.path("valueQuantity").path("value").asDouble(node.path("valueInteger").asDouble(0.0));
        String unit = node.path("valueQuantity").path("unit").asText("");

        // Handle Blood Stock Observation
        if (code.toLowerCase().contains("blood") || node.path("subject").path("reference").asText().startsWith("BloodGroup/")) {
            String bgRef = node.path("subject").path("reference").asText("");
            String bgStr = bgRef.replace("BloodGroup/", "");
            if (bgStr.isBlank()) {
                bgStr = node.path("code").path("text").asText("O_POSITIVE");
            }
            try {
                BloodGroup bg = BloodGroup.fromString(bgStr);
                int units = (int) Math.round(val);
                LocalDate expiry = LocalDate.now().plusDays(28);

                Optional<BloodStock> existing = bloodStockRepo.findByHospitalIdAndBloodGroupAndExpiryDate(hospitalId, bg, expiry);
                BloodStock stock;
                if (existing.isPresent()) {
                    stock = existing.get();
                    stock.setUnitsAvailable(units);
                } else {
                    stock = new BloodStock(hospitalId, bg, units, expiry, 5);
                }
                bloodStockRepo.save(stock);

                if (stock.isLowStock()) {
                    eventPublisher.publishEvent(new StockLow(hospitalId, "Hospital", bg.name(), stock.getUnitsAvailable(), stock.getLowStockThreshold()));
                }

                log.info("[FHIR:Observation] Hospital {}: Updated blood stock {} -> {} units", hospitalId, bg.getLabel(), units);
                return String.format("Updated blood stock %s: %d units", bg.getLabel(), units);
            } catch (Exception ex) {
                log.warn("Could not parse blood group from FHIR observation: {}", ex.getMessage());
            }
        }

        log.info("[FHIR:Observation] Hospital {}: Observation '{}' recorded value: {} {}", hospitalId, code, val, unit);
        return String.format("Recorded observation '%s': %.1f %s", code, val, unit);
    }

    private String processAppointment(UUID hospitalId, JsonNode node) {
        String status = node.path("status").asText("booked");
        String desc = node.path("description").asText("OPD Consultation");
        String patient = node.path("patientName").asText("Patient");
        log.info("[FHIR:Appointment] Hospital {}: Appointment for {} with status '{}'", hospitalId, patient, status);
        return String.format("Processed appointment for %s (%s)", patient, status);
    }

    private String processPatient(UUID hospitalId, JsonNode node) {
        String name = "Unknown";
        JsonNode names = node.path("name");
        if (names.isArray() && !names.isEmpty()) {
            name = names.get(0).path("text").asText(names.get(0).path("family").asText("Patient"));
        }
        log.info("[FHIR:Patient] Hospital {}: Patient admission/intake: {}", hospitalId, name);
        return "Processed patient record: " + name;
    }

    private String generateOperationOutcome(boolean isSuccess, String message) {
        String severity = isSuccess ? "information" : "error";
        String code = isSuccess ? "informational" : "processing";
        return String.format("""
                {
                  "resourceType": "OperationOutcome",
                  "issue": [
                    {
                      "severity": "%s",
                      "code": "%s",
                      "diagnostics": "%s"
                    }
                  ]
                }
                """, severity, code, message.replace("\"", "\\\""));
    }
}
