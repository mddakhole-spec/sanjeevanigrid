package com.sanjeevanigrid.simulator.domain;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.*;

/**
 * Generates 90 days of hourly synthetic hospital data per ward.
 * Poisson arrivals with weekday/hour patterns and injected surge events.
 */
public class SyntheticDataEngine {

    private final Random rng;

    public SyntheticDataEngine(long seed) {
        this.rng = new Random(seed);
    }

    public SyntheticDataEngine() {
        this(42L);
    }

    /**
     * Generate hourly admissions for a single ward over 90 days.
     *
     * @param wardName     ward identifier
     * @param baseLambda   base Poisson λ (mean arrivals per hour)
     * @param totalBeds    total beds in ward
     * @param totalIcu     total ICU beds
     * @param totalVents   total ventilators
     * @param staffPerShift staff per 8h shift
     * @param oxygenUnits  total oxygen units
     * @param surgeEvents  list of surge event descriptors
     * @return list of hourly data points (90 * 24 = 2160 entries)
     */
    public List<HourlyDataPoint> generate(
            String wardName, double baseLambda,
            int totalBeds, int totalIcu, int totalVents,
            int staffPerShift, double oxygenUnits,
            List<SurgeEventDescriptor> surgeEvents) {

        List<HourlyDataPoint> data = new ArrayList<>(2160);
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);

        int occupiedBeds = (int)(totalBeds * 0.45);
        int occupiedIcu = (int)(totalIcu * 0.30);
        int usedVents = (int)(totalVents * 0.25);
        double oxygenRemaining = oxygenUnits;

        for (int hour = 0; hour < 2160; hour++) {
            LocalDateTime ts = start.plusHours(hour);

            // weekday/hour multiplier
            double weekdayFactor = weekdayMultiplier(ts.getDayOfWeek());
            double hourFactor = hourMultiplier(ts.getHour());

            // surge multiplier
            double surgeFactor = 1.0;
            for (SurgeEventDescriptor se : surgeEvents) {
                if (hour >= se.startHour() && hour < se.startHour() + se.durationHours()) {
                    double progress = (double)(hour - se.startHour()) / se.durationHours();
                    // bell curve surge
                    surgeFactor = Math.max(surgeFactor,
                            1.0 + (se.peakMultiplier() - 1.0) * Math.sin(Math.PI * progress));
                }
            }

            double lambda = baseLambda * weekdayFactor * hourFactor * surgeFactor;
            int admissions = poissonSample(lambda);
            int discharges = poissonSample(baseLambda * 0.35 * hourFactor);

            // update occupied counts
            occupiedBeds = Math.max(0, Math.min(totalBeds, occupiedBeds + admissions - discharges));
            int icuNew = (int)(admissions * 0.15);
            int icuDischarge = (int)(discharges * 0.12);
            occupiedIcu = Math.max(0, Math.min(totalIcu, occupiedIcu + icuNew - icuDischarge));
            int ventNew = (int)(admissions * 0.08);
            int ventRelease = (int)(discharges * 0.07);
            usedVents = Math.max(0, Math.min(totalVents, usedVents + ventNew - ventRelease));

            // staff follows 3 shifts per day pattern
            int currentStaff = staffForHour(staffPerShift, ts.getHour());

            // oxygen depletes under load, replenished daily at 6am
            double oxygenUsage = occupiedBeds * 0.5 + occupiedIcu * 2.0 + usedVents * 3.0;
            oxygenRemaining = Math.max(0, oxygenRemaining - oxygenUsage / 24.0);
            if (ts.getHour() == 6) {
                oxygenRemaining = Math.min(oxygenUnits, oxygenRemaining + oxygenUnits * 0.4);
            }
            double oxygenPct = (oxygenRemaining / oxygenUnits) * 100.0;

            data.add(new HourlyDataPoint(
                    ts, wardName, admissions, discharges,
                    totalBeds, occupiedBeds,
                    totalIcu, occupiedIcu,
                    totalVents, usedVents,
                    currentStaff, Math.round(oxygenPct * 100.0) / 100.0
            ));
        }
        return data;
    }

    /**
     * Generate a default hospital dataset with 4 wards and preset surge events.
     */
    public Map<String, List<HourlyDataPoint>> generateDefaultHospital() {
        Map<String, List<HourlyDataPoint>> hospital = new LinkedHashMap<>();

        // Surge events injected into the dataset
        List<SurgeEventDescriptor> surges = List.of(
                new SurgeEventDescriptor(720, 72, 3.5, "Monsoon Flooding"),   // day 30
                new SurgeEventDescriptor(1440, 48, 2.8, "Dengue Outbreak"),   // day 60
                new SurgeEventDescriptor(1920, 12, 4.0, "Highway Pile-up")    // day 80
        );

        hospital.put("General", generate("General", 4.5, 60, 8, 4, 12, 5000, surges));
        hospital.put("Pediatric", generate("Pediatric", 2.2, 30, 4, 2, 8, 2500, surges));
        hospital.put("Maternity", generate("Maternity", 1.8, 25, 3, 2, 6, 2000, surges));
        hospital.put("Emergency", generate("Emergency", 5.0, 20, 10, 6, 15, 6000, surges));

        return hospital;
    }

    // ---- helpers ----

    private double weekdayMultiplier(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> 1.15;
            case TUESDAY -> 1.10;
            case WEDNESDAY -> 1.05;
            case THURSDAY -> 1.08;
            case FRIDAY -> 1.20;
            case SATURDAY -> 0.85;
            case SUNDAY -> 0.70;
        };
    }

    private double hourMultiplier(int hour) {
        // peak at 10am-2pm and 6-8pm, low overnight
        if (hour >= 0 && hour < 6) return 0.3;
        if (hour >= 6 && hour < 9) return 0.7;
        if (hour >= 9 && hour < 14) return 1.4;
        if (hour >= 14 && hour < 17) return 1.0;
        if (hour >= 17 && hour < 21) return 1.3;
        return 0.5; // 21-24
    }

    private int staffForHour(int baseStaff, int hour) {
        if (hour >= 7 && hour < 15) return baseStaff;         // morning shift
        if (hour >= 15 && hour < 23) return (int)(baseStaff * 0.85); // evening shift
        return (int)(baseStaff * 0.6);                         // night shift
    }

    private int poissonSample(double lambda) {
        if (lambda <= 0) return 0;
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= rng.nextDouble();
        } while (p > L);
        return k - 1;
    }
}
