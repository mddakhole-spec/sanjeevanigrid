package com.sanjeevanigrid.equity.domain;

import org.springframework.stereotype.Component;

/**
 * Calculates Village Healthcare Access Score (0 - 100) and categorizes vulnerability.
 *
 * Scoring Weights:
 * - 40% Travel Time to nearest hospital facility
 * - 30% Doctor Availability / Local Health Worker index
 * - 30% Recent Medical Camp Coverage (penalizes villages with no recent camp)
 *
 * Categories:
 * - RED (< 40.0): Critical Access Gap
 * - YELLOW (40.0 - 70.0): Moderate Vulnerability
 * - GREEN (> 70.0): Good Healthcare Access
 */
@Component
public class EquityCalculator {

    public double calculateAccessScore(int travelTimeMinutes, double doctorAvailabilityScore, int daysSinceLastCamp) {
        // 1. Travel time score: 0 min -> 100, 120+ min -> 0
        double travelScore = Math.max(0.0, 100.0 - (travelTimeMinutes * 0.85));

        // 2. Doctor availability score: 1.0 -> 100, 0.0 -> 0
        double doctorScore = Math.min(100.0, Math.max(0.0, doctorAvailabilityScore * 100.0));

        // 3. Camp recency score: recent camp (< 30 days) -> 100, 300+ days -> 0
        double campScore = Math.max(0.0, 100.0 - (Math.min(300, daysSinceLastCamp) / 300.0) * 100.0);

        // Weighted combination: 40% travel, 30% doctor, 30% camp recency
        double rawScore = (0.40 * travelScore) + (0.30 * doctorScore) + (0.30 * campScore);

        return Math.round(rawScore * 10.0) / 10.0;
    }

    public String determineVulnerabilityCategory(double accessScore) {
        if (accessScore < 40.0) {
            return "RED";
        } else if (accessScore <= 70.0) {
            return "YELLOW";
        } else {
            return "GREEN";
        }
    }

    public void recalculateVillageEquity(Village village) {
        double score = calculateAccessScore(
                village.getTravelTimeMinutes(),
                village.getDoctorAvailabilityScore(),
                village.getDaysSinceLastCamp()
        );
        village.setAccessScore(score);
        village.setVulnerabilityCategory(determineVulnerabilityCategory(score));
    }
}

