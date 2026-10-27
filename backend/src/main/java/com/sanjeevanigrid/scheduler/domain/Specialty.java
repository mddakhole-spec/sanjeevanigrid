package com.sanjeevanigrid.scheduler.domain;

public enum Specialty {
    GENERAL_MEDICINE("General Medicine", "सामान्य औषधोपचार"),
    PEDIATRICS("Pediatrics", "बालरोग"),
    ORTHOPEDICS("Orthopedics", "अस्थिरोग"),
    GYNECOLOGY("Gynecology & Obstetrics", "स्त्रीरोग"),
    CARDIOLOGY("Cardiology", "हृदयरोग"),
    EMERGENCY("Emergency & Trauma", "आपत्कालीन व ट्रॉमा");

    private final String displayNameEn;
    private final String displayNameMr;

    Specialty(String displayNameEn, String displayNameMr) {
        this.displayNameEn = displayNameEn;
        this.displayNameMr = displayNameMr;
    }

    public String getDisplayNameEn() {
        return displayNameEn;
    }

    public String getDisplayNameMr() {
        return displayNameMr;
    }
}
