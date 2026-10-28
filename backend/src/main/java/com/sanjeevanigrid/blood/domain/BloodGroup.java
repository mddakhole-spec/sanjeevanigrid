package com.sanjeevanigrid.blood.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Blood group enumeration with standard Red Blood Cell compatibility matrix.
 */
public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Determines whether this blood group can donate red blood cells to the given recipient.
     */
    public boolean canDonateTo(BloodGroup recipient) {
        if (recipient == null) return false;
        return switch (this) {
            case O_NEGATIVE -> true; // Universal donor
            case O_POSITIVE -> recipient == O_POSITIVE || recipient == A_POSITIVE || recipient == B_POSITIVE || recipient == AB_POSITIVE;
            case A_NEGATIVE -> recipient == A_NEGATIVE || recipient == A_POSITIVE || recipient == AB_NEGATIVE || recipient == AB_POSITIVE;
            case A_POSITIVE -> recipient == A_POSITIVE || recipient == AB_POSITIVE;
            case B_NEGATIVE -> recipient == B_NEGATIVE || recipient == B_POSITIVE || recipient == AB_NEGATIVE || recipient == AB_POSITIVE;
            case B_POSITIVE -> recipient == B_POSITIVE || recipient == AB_POSITIVE;
            case AB_NEGATIVE -> recipient == AB_NEGATIVE || recipient == AB_POSITIVE;
            case AB_POSITIVE -> recipient == AB_POSITIVE; // Only AB+
        };
    }

    /**
     * Determines whether this blood group can receive red blood cells from the given donor.
     */
    public boolean canReceiveFrom(BloodGroup donor) {
        if (donor == null) return false;
        return donor.canDonateTo(this);
    }

    /**
     * Returns list of compatible donor groups that this group can receive from.
     */
    public List<BloodGroup> getCompatibleDonors() {
        return Arrays.stream(BloodGroup.values())
                .filter(this::canReceiveFrom)
                .toList();
    }

    /**
     * Parse string representation like "A+", "A_POSITIVE", "a+", "O-", etc.
     */
    public static BloodGroup fromString(String val) {
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Blood group cannot be empty");
        }
        String clean = val.trim().toUpperCase().replace(" ", "_");
        for (BloodGroup bg : values()) {
            if (bg.name().equals(clean) || bg.label.equalsIgnoreCase(val.trim())) {
                return bg;
            }
        }
        // Fallback replacement of symbols
        clean = clean.replace("+", "_POSITIVE").replace("-", "_NEGATIVE");
        for (BloodGroup bg : values()) {
            if (bg.name().equals(clean)) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Unknown blood group: " + val);
    }
}
