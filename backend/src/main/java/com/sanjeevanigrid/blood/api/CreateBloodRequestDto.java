package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBloodRequestDto(
        @NotNull UUID hospitalId,
        @NotBlank String patientName,
        String patientPhone,
        @NotNull BloodGroup bloodGroup,
        @Min(1) int unitsNeeded,
        String urgency,
        Double radiusKm
) {}
