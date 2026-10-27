package com.sanjeevanigrid.scheduler.domain;

public enum AppointmentPriority {
    ROUTINE(1, "Routine", "नियमित"),
    URGENT(2, "Urgent", "तातडीचे"),
    EMERGENCY(3, "Emergency", "अतितातडीचे / आपत्कालीन");

    private final int level;
    private final String labelEn;
    private final String labelMr;

    AppointmentPriority(int level, String labelEn, String labelMr) {
        this.level = level;
        this.labelEn = labelEn;
        this.labelMr = labelMr;
    }

    public int getLevel() {
        return level;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelMr() {
        return labelMr;
    }

    public boolean canBump(AppointmentPriority other) {
        return this.level > other.level;
    }
}
