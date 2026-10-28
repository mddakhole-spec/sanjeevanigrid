package com.sanjeevanigrid.blood.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Checks donor eligibility against the mandatory cool-down period (standard: 90 days).
 */
@Component
public class EligibilityCalculator {

    public record EligibilityResult(
            boolean isEligible,
            long daysSinceDonation,
            long daysRemainingInCooldown,
            LocalDate eligibleDate,
            String reason
    ) {}

    private final int cooldownDays;

    public EligibilityCalculator(@Value("${app.blood.cooldown-days:90}") int cooldownDays) {
        this.cooldownDays = cooldownDays;
    }

    public EligibilityResult checkEligibility(BloodDonor donor) {
        return checkEligibility(donor, LocalDate.now());
    }

    public EligibilityResult checkEligibility(BloodDonor donor, LocalDate referenceDate) {
        if (donor == null) {
            return new EligibilityResult(false, -1, 0, referenceDate, "Donor does not exist");
        }
        if (!donor.isAvailable()) {
            return new EligibilityResult(false, -1, 0, null, "Donor availability toggle is set to OFF");
        }
        if (!donor.isConsentGiven()) {
            return new EligibilityResult(false, -1, 0, null, "Donor has not consented to emergency alerts");
        }

        LocalDate lastDonation = donor.getLastDonationDate();
        if (lastDonation == null) {
            return new EligibilityResult(true, -1, 0, referenceDate, "Eligible (First time donor)");
        }

        long daysPassed = ChronoUnit.DAYS.between(lastDonation, referenceDate);
        if (daysPassed >= cooldownDays) {
            return new EligibilityResult(true, daysPassed, 0, referenceDate, "Eligible (Cool-down completed)");
        } else {
            long remaining = cooldownDays - daysPassed;
            LocalDate nextEligible = lastDonation.plusDays(cooldownDays);
            return new EligibilityResult(false, daysPassed, remaining, nextEligible,
                    "In cool-down: " + remaining + " days remaining (eligible on " + nextEligible + ")");
        }
    }

    public int getCooldownDays() {
        return cooldownDays;
    }
}
