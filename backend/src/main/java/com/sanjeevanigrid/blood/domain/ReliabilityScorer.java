package com.sanjeevanigrid.blood.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Computes donor reliability score:
 * Reliability Score = (0.5 * response_rate) + (0.3 * fulfilment_rate) + (0.2 * recency_score)
 * Returns a score between 0.00 and 1.00 used for ranking donors during emergency alerts.
 */
@Component
public class ReliabilityScorer {

    public record ReliabilityBreakdown(
            double overallScore,
            double responseRate,
            double fulfilmentRate,
            double recencyScore,
            long daysSinceLastResponse
    ) {}

    public double calculate(BloodDonor donor) {
        return evaluate(donor, Instant.now()).overallScore();
    }

    public ReliabilityBreakdown evaluate(BloodDonor donor, Instant referenceTime) {
        if (donor == null) {
            return new ReliabilityBreakdown(0.0, 0.0, 0.0, 0.0, 999);
        }

        // 1. Response Rate (Weight: 0.5)
        double responseRate;
        if (donor.getTotalAlertsReceived() == 0) {
            responseRate = 0.90; // Default baseline for new registered donors
        } else {
            responseRate = Math.min(1.0, Math.max(0.0,
                    (double) donor.getTotalAlertsResponded() / donor.getTotalAlertsReceived()));
        }

        // 2. Fulfilment Rate (Weight: 0.3)
        double fulfilmentRate;
        if (donor.getTotalAlertsResponded() == 0) {
            fulfilmentRate = (donor.getTotalAlertsReceived() == 0) ? 0.85 : 0.0;
        } else {
            fulfilmentRate = Math.min(1.0, Math.max(0.0,
                    (double) donor.getTotalDonationsFulfilled() / donor.getTotalAlertsResponded()));
        }

        // 3. Recency Score (Weight: 0.2)
        double recencyScore;
        long daysSinceLastResponse;
        if (donor.getLastResponseAt() == null) {
            daysSinceLastResponse = (donor.getTotalAlertsReceived() == 0) ? 0 : 120;
            recencyScore = (donor.getTotalAlertsReceived() == 0) ? 0.80 : 0.20;
        } else {
            Duration duration = Duration.between(donor.getLastResponseAt(), referenceTime);
            daysSinceLastResponse = Math.max(0, duration.toDays());

            if (daysSinceLastResponse <= 7) {
                recencyScore = 1.00;
            } else if (daysSinceLastResponse <= 14) {
                recencyScore = 0.90;
            } else if (daysSinceLastResponse <= 30) {
                recencyScore = 0.75;
            } else if (daysSinceLastResponse <= 60) {
                recencyScore = 0.50;
            } else if (daysSinceLastResponse <= 90) {
                recencyScore = 0.35;
            } else {
                recencyScore = 0.20;
            }
        }

        double overallScore = (0.5 * responseRate) + (0.3 * fulfilmentRate) + (0.2 * recencyScore);
        // Round to 3 decimal places
        overallScore = Math.round(overallScore * 1000.0) / 1000.0;

        return new ReliabilityBreakdown(overallScore, responseRate, fulfilmentRate, recencyScore, daysSinceLastResponse);
    }
}
