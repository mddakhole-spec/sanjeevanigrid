package com.sanjeevanigrid.integration.domain;

import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodDonorRepository;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CsvValidationEngineTest {

    private BloodStockRepository stockRepo;
    private BloodDonorRepository donorRepo;
    private CsvValidationEngine engine;

    @BeforeEach
    void setUp() {
        stockRepo = mock(BloodStockRepository.class);
        donorRepo = mock(BloodDonorRepository.class);
        engine = new CsvValidationEngine(stockRepo, donorRepo);
    }

    @Test
    @DisplayName("Previews valid and invalid rows in blood stock CSV")
    void testPreviewBloodStockCsv() {
        String csv = """
                bloodGroup,unitsAvailable,expiryDate,lowStockThreshold
                A+,15,2026-12-01,5
                INVALID_GROUP,10,2026-12-01,5
                B-,not_a_number,2026-12-01,5
                O-,2,2026-11-15,5
                """;

        CsvValidationEngine.CsvPreviewResult result = engine.previewCsv("BLOOD_STOCK", csv, Map.of());

        assertThat(result.totalRows()).isEqualTo(4);
        assertThat(result.validRows()).isEqualTo(2); // rows 1 and 4
        assertThat(result.invalidRows()).isEqualTo(2); // rows 2 and 3
        assertThat(result.errors()).hasSize(2);

        assertThat(result.previewRows().get(0).status()).isEqualTo("VALID");
        assertThat(result.previewRows().get(1).status()).isEqualTo("INVALID");
        assertThat(result.previewRows().get(1).rowErrors().get(0)).contains("Invalid blood group");
        assertThat(result.previewRows().get(2).status()).isEqualTo("INVALID");
        assertThat(result.previewRows().get(2).rowErrors().get(0)).contains("Must be a valid integer");
        assertThat(result.previewRows().get(3).status()).isEqualTo("VALID");
    }

    @Test
    @DisplayName("Commits only valid rows from CSV into database")
    void testCommitBloodStockCsv() {
        String csv = """
                bloodGroup,unitsAvailable,expiryDate,lowStockThreshold
                A+,15,2026-12-01,5
                INVALID_GROUP,10,2026-12-01,5
                O-,2,2026-11-15,5
                """;

        when(stockRepo.findByHospitalIdAndBloodGroupAndExpiryDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(stockRepo.save(any(BloodStock.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID hospitalId = UUID.randomUUID();
        int committed = engine.commitCsv(hospitalId, "BLOOD_STOCK", csv, Map.of());

        assertThat(committed).isEqualTo(2);
        verify(stockRepo, times(2)).save(any(BloodStock.class));
    }
}
