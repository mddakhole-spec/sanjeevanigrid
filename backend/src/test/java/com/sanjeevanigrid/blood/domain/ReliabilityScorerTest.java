package com.sanjeevanigrid.blood.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ReliabilityScorerTest {

    private final ReliabilityScorer scorer = new ReliabilityScorer();

    @Test
    @DisplayName("New donor with 0 alerts receives fair baseline score")
    void testNewDonorBaseline() {
        BloodDonor donor = new BloodDonor("New Donor", "+919822000001", BloodGroup.O_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setTotalAlertsReceived(0);
        donor.setTotalAlertsResponded(0);
        donor.setTotalDonationsFulfilled(0);

        ReliabilityScorer.ReliabilityBreakdown breakdown = scorer.evaluate(donor, Instant.now());

        // (0.5 * 0.90) + (0.3 * 0.85) + (0.2 * 0.80) = 0.45 + 0.255 + 0.16 = 0.865
        assertThat(breakdown.overallScore()).isEqualTo(0.865);
        assertThat(breakdown.responseRate()).isEqualTo(0.90);
        assertThat(breakdown.fulfilmentRate()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("Perfect donor with 100% response, 100% fulfilment, recent response achieves 1.0")
    void testPerfectDonor() {
        BloodDonor donor = new BloodDonor("Star Donor", "+919822000002", BloodGroup.A_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setTotalAlertsReceived(10);
        donor.setTotalAlertsResponded(10);
        donor.setTotalDonationsFulfilled(10);
        donor.setLastResponseAt(Instant.now().minus(2, ChronoUnit.DAYS)); // within 7 days -> 1.0

        ReliabilityScorer.ReliabilityBreakdown breakdown = scorer.evaluate(donor, Instant.now());

        // (0.5 * 1.0) + (0.3 * 1.0) + (0.2 * 1.0) = 1.0
        assertThat(breakdown.overallScore()).isEqualTo(1.0);
        assertThat(breakdown.responseRate()).isEqualTo(1.0);
        assertThat(breakdown.fulfilmentRate()).isEqualTo(1.0);
        assertThat(breakdown.recencyScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Formula weights strictly adhere to (0.5 response + 0.3 fulfilment + 0.2 recency)")
    void testSpecificFormulaWeights() {
        BloodDonor donor = new BloodDonor("Test Donor", "+919822000003", BloodGroup.B_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        // 8/10 response = 0.80
        donor.setTotalAlertsReceived(10);
        donor.setTotalAlertsResponded(8);
        // 4/8 fulfillment = 0.50
        donor.setTotalDonationsFulfilled(4);
        // 20 days ago -> 0.75 recency
        donor.setLastResponseAt(Instant.now().minus(20, ChronoUnit.DAYS));

        ReliabilityScorer.ReliabilityBreakdown breakdown = scorer.evaluate(donor, Instant.now());

        // (0.5 * 0.80) + (0.3 * 0.50) + (0.2 * 0.75) = 0.40 + 0.15 + 0.15 = 0.70
        assertThat(breakdown.overallScore()).isEqualTo(0.70);
    }

    @Test
    @DisplayName("Inactive donor recency decays when response was over 90 days ago")
    void testRecencyDecay() {
        BloodDonor donor = new BloodDonor("Old Donor", "+919822000004", BloodGroup.AB_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setTotalAlertsReceived(5);
        donor.setTotalAlertsResponded(5);
        donor.setTotalDonationsFulfilled(5);
        donor.setLastResponseAt(Instant.now().minus(100, ChronoUnit.DAYS));

        ReliabilityScorer.ReliabilityBreakdown breakdown = scorer.evaluate(donor, Instant.now());

        assertThat(breakdown.recencyScore()).isEqualTo(0.20);
        // (0.5 * 1.0) + (0.3 * 1.0) + (0.2 * 0.20) = 0.5 + 0.3 + 0.04 = 0.84
        assertThat(breakdown.overallScore()).isEqualTo(0.84);
    }
}
