package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDonorDto(
        @NotBlank String fullName,
        @NotBlank String phone,
        String email,
        @NotNull BloodGroup bloodGroup,
        double latitude,
        double longitude,
        String address,
        String district,
        boolean consentGiven
) {}
