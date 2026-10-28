package com.sanjeevanigrid.integration.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FhirPayloadParserTest {

    private BloodStockRepository stockRepo;
    private ApplicationEventPublisher eventPublisher;
    private FhirPayloadParser parser;

    private final UUID hospitalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stockRepo = mock(BloodStockRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        parser = new FhirPayloadParser(new ObjectMapper(), stockRepo, eventPublisher);
    }

    @Test
    @DisplayName("Parses FHIR Location resource and updates ward capacity summary")
    void testParseLocation() {
        String json = """
                {
                  "resourceType": "Location",
                  "name": "ICU Ward",
                  "status": "active",
                  "totalBeds": 25,
                  "availableBeds": 6
                }
                """;

        FhirPayloadParser.ParsedFhirSummary summary = parser.parseAndApply(hospitalId, json);

        assertThat(summary.success()).isTrue();
        assertThat(summary.primaryResourceType()).isEqualTo("Location");
        assertThat(summary.entriesProcessed()).isEqualTo(1);
        assertThat(summary.operationOutcomeJson()).contains("OperationOutcome");
        assertThat(summary.actionsTaken().get(0)).contains("Updated Ward 'ICU Ward': 6/25 beds available");
    }

    @Test
    @DisplayName("Parses FHIR Observation resource for blood-stock and updates inventory")
    void testParseBloodObservation() {
        String json = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": {
                    "coding": [
                      {
                        "system": "http://sanjeevanigrid.in/codes",
                        "code": "blood-stock",
                        "display": "Blood Stock Level"
                      }
                    ]
                  },
                  "subject": {
                    "reference": "BloodGroup/O_NEGATIVE"
                  },
                  "valueQuantity": {
                    "value": 12,
                    "unit": "units"
                  }
                }
                """;

        BloodStock stock = new BloodStock(hospitalId, BloodGroup.O_NEGATIVE, 4, LocalDate.now().plusDays(28), 5);
        when(stockRepo.findByHospitalIdAndBloodGroupAndExpiryDate(eq(hospitalId), eq(BloodGroup.O_NEGATIVE), any(LocalDate.class)))
                .thenReturn(Optional.of(stock));

        FhirPayloadParser.ParsedFhirSummary summary = parser.parseAndApply(hospitalId, json);

        assertThat(summary.success()).isTrue();
        assertThat(summary.primaryResourceType()).isEqualTo("Observation");
        assertThat(stock.getUnitsAvailable()).isEqualTo(12);
        verify(stockRepo).save(stock);
    }

    @Test
    @DisplayName("Parses FHIR Bundle with multiple entries")
    void testParseBundle() {
        String json = """
                {
                  "resourceType": "Bundle",
                  "type": "transaction",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Location",
                        "name": "Emergency Ward",
                        "totalBeds": 15,
                        "availableBeds": 3
                      }
                    },
                    {
                      "resource": {
                        "resourceType": "Patient",
                        "name": [
                          {
                            "text": "Ramesh Patil"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        FhirPayloadParser.ParsedFhirSummary summary = parser.parseAndApply(hospitalId, json);

        assertThat(summary.success()).isTrue();
        assertThat(summary.entriesProcessed()).isEqualTo(2);
        assertThat(summary.actionsTaken()).hasSize(2);
    }
}
