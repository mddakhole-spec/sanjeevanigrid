package com.sanjeevanigrid.blood.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BloodGroupCompatibilityTest {

    @Test
    @DisplayName("O_NEGATIVE is universal red blood cell donor to all 8 blood groups")
    void testUniversalDonor() {
        BloodGroup oNeg = BloodGroup.O_NEGATIVE;
        for (BloodGroup recipient : BloodGroup.values()) {
            assertThat(oNeg.canDonateTo(recipient))
                    .as("O- should be able to donate to " + recipient)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("AB_POSITIVE is universal recipient from all 8 blood groups")
    void testUniversalRecipient() {
        BloodGroup abPos = BloodGroup.AB_POSITIVE;
        for (BloodGroup donor : BloodGroup.values()) {
            assertThat(abPos.canReceiveFrom(donor))
                    .as("AB+ should be able to receive from " + donor)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("A_POSITIVE donation and reception rules are medically correct")
    void testAPositiveRules() {
        BloodGroup aPos = BloodGroup.A_POSITIVE;

        // Can donate to: A+, AB+
        assertThat(aPos.canDonateTo(BloodGroup.A_POSITIVE)).isTrue();
        assertThat(aPos.canDonateTo(BloodGroup.AB_POSITIVE)).isTrue();
        assertThat(aPos.canDonateTo(BloodGroup.B_POSITIVE)).isFalse();
        assertThat(aPos.canDonateTo(BloodGroup.O_POSITIVE)).isFalse();

        // Can receive from: A+, A-, O+, O-
        assertThat(aPos.canReceiveFrom(BloodGroup.A_POSITIVE)).isTrue();
        assertThat(aPos.canReceiveFrom(BloodGroup.A_NEGATIVE)).isTrue();
        assertThat(aPos.canReceiveFrom(BloodGroup.O_POSITIVE)).isTrue();
        assertThat(aPos.canReceiveFrom(BloodGroup.O_NEGATIVE)).isTrue();
        assertThat(aPos.canReceiveFrom(BloodGroup.B_POSITIVE)).isFalse();
    }

    @Test
    @DisplayName("String parsing handles symbols and enum names correctly")
    void testFromString() {
        assertThat(BloodGroup.fromString("A+")).isEqualTo(BloodGroup.A_POSITIVE);
        assertThat(BloodGroup.fromString("a-")).isEqualTo(BloodGroup.A_NEGATIVE);
        assertThat(BloodGroup.fromString("O_NEGATIVE")).isEqualTo(BloodGroup.O_NEGATIVE);
        assertThat(BloodGroup.fromString("AB+")).isEqualTo(BloodGroup.AB_POSITIVE);
        assertThat(BloodGroup.fromString("B_POSITIVE")).isEqualTo(BloodGroup.B_POSITIVE);
    }
}
