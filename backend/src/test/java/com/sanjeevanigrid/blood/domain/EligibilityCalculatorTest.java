package com.sanjeevanigrid.blood.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityCalculatorTest {

    private final EligibilityCalculator calculator = new EligibilityCalculator(90);

    @Test
    @DisplayName("First-time donor with null lastDonationDate is eligible immediately")
    void testFirstTimeDonorEligible() {
        BloodDonor donor = new BloodDonor("First Timer", "+919822000010", BloodGroup.O_NEGATIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setLastDonationDate(null);

        EligibilityCalculator.EligibilityResult res = calculator.checkEligibility(donor, LocalDate.of(2026, 11, 1));
        assertThat(res.isEligible()).isTrue();
        assertThat(res.daysRemainingInCooldown()).isEqualTo(0);
    }

    @Test
    @DisplayName("Donor who donated 30 days ago is ineligible with 60 days remaining in cool-down")
    void testDonorInCooldown() {
        BloodDonor donor = new BloodDonor("Recent Donor", "+919822000011", BloodGroup.A_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setLastDonationDate(LocalDate.of(2026, 10, 1));

        EligibilityCalculator.EligibilityResult res = calculator.checkEligibility(donor, LocalDate.of(2026, 10, 31));
        assertThat(res.isEligible()).isFalse();
        assertThat(res.daysSinceDonation()).isEqualTo(30);
        assertThat(res.daysRemainingInCooldown()).isEqualTo(60);
        assertThat(res.eligibleDate()).isEqualTo(LocalDate.of(2026, 10, 1).plusDays(90));
    }

    @Test
    @DisplayName("Donor who donated 95 days ago has completed cool-down and is eligible")
    void testDonorCompletedCooldown() {
        BloodDonor donor = new BloodDonor("Cooled Down", "+919822000012", BloodGroup.B_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setLastDonationDate(LocalDate.of(2026, 7, 1));

        EligibilityCalculator.EligibilityResult res = calculator.checkEligibility(donor, LocalDate.of(2026, 10, 5));
        assertThat(res.isEligible()).isTrue();
        assertThat(res.daysRemainingInCooldown()).isEqualTo(0);
    }

    @Test
    @DisplayName("Donor with availability toggled to false is not eligible")
    void testUnavailableDonorIneligible() {
        BloodDonor donor = new BloodDonor("Unavailable", "+919822000013", BloodGroup.O_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setAvailable(false);

        EligibilityCalculator.EligibilityResult res = calculator.checkEligibility(donor, LocalDate.of(2026, 11, 1));
        assertThat(res.isEligible()).isFalse();
        assertThat(res.reason()).contains("availability toggle");
    }

    @Test
    @DisplayName("Donor without consent is excluded from eligibility")
    void testUnconsentedDonorIneligible() {
        BloodDonor donor = new BloodDonor("No Consent", "+919822000014", BloodGroup.O_POSITIVE, 20.38, 78.12, "Yavatmal", "Yavatmal");
        donor.setConsentGiven(false);

        EligibilityCalculator.EligibilityResult res = calculator.checkEligibility(donor, LocalDate.of(2026, 11, 1));
        assertThat(res.isEligible()).isFalse();
        assertThat(res.reason()).contains("consented");
    }
}
