package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CampRsvpRequestDto(
        @NotBlank String donorName,
        @NotBlank String donorPhone,
        @NotNull BloodGroup bloodGroup,
        String notes
) {}
