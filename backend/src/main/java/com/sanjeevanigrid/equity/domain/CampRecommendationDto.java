package com.sanjeevanigrid.equity.domain;

import java.time.LocalDate;
import java.util.UUID;

public record CampRecommendationDto(
        int rank,
        UUID villageId,
        String villageName,
        String taluka,
        String district,
        int population,
        double accessScore,
        String vulnerabilityCategory,
        int daysSinceLastCamp,
        double needScore,
        String suggestedVenue,
        int suggestedTargetAttendees,
        LocalDate suggestedCampDate,
        String urgencyReason,
        Double latitude,
        Double longitude
) {}

