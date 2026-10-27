package com.sanjeevanigrid.simulator.domain;

import java.util.*;

/**
 * Scenario engine: applies surge multiplier, triage mode, extra beds,
 * staff reassignment and runs hour-by-hour simulation.
 * Computes KPIs: patients turned away, peak occupancy, hours above 90%.
 */
public class ScenarioEngine {

    /**
     * Run a scenario simulation.
     *
     * @param baseline     baseline hourly data for a ward (or aggregated)
     * @param params       scenario parameters
     * @return ScenarioResult with hour-by-hour simulation and KPIs
     */
    public ScenarioResult run(List<HourlyDataPoint> baseline, ScenarioParams params) {
        int duration = Math.min(params.durationHours(), baseline.size());

        List<SimulatedHour> hours = new ArrayList<>(duration);
        int totalTurnedAway = 0;
        double peakOccupancy = 0;
        int hoursAbove90 = 0;

        // take the last `duration` hours as the baseline window
        int startIdx = Math.max(0, baseline.size() - duration);

        for (int h = 0; h < duration; h++) {
            HourlyDataPoint bp = baseline.get(startIdx + h);

            // Apply surge multiplier to admissions
            int boostedAdmissions = (int) Math.ceil(bp.admissions() * params.surgeMultiplier());

            // Triage mode reduces admissions that need full beds
            double triageReduction = switch (params.triageMode()) {
                case "STRICT" -> 0.70;
                case "MODERATE" -> 0.85;
                default -> 1.0; // NORMAL
            };
            int effectiveAdmissions = (int) Math.ceil(boostedAdmissions * triageReduction);

            // Extra beds and staff
            int effectiveTotalBeds = bp.totalBeds() + params.extraBeds();
            int effectiveStaff = bp.staffOnDuty() + params.staffReassignment();

            // Staff-limited capacity: each staff can handle ~5 patients
            int staffCapacity = effectiveStaff * 5;
            int effectiveCapacity = Math.min(effectiveTotalBeds, staffCapacity);

            // Simulate occupancy
            int prevOccupied = h > 0 ? hours.get(h - 1).occupiedBeds() : bp.occupiedBeds();
            int newOccupied = prevOccupied + effectiveAdmissions - bp.discharges();

            int turnedAway = 0;
            if (newOccupied > effectiveCapacity) {
                turnedAway = newOccupied - effectiveCapacity;
                newOccupied = effectiveCapacity;
            }
            newOccupied = Math.max(0, newOccupied);

            double occupancyPct = effectiveTotalBeds > 0
                    ? (double) newOccupied / effectiveTotalBeds * 100.0 : 0;

            totalTurnedAway += turnedAway;
            peakOccupancy = Math.max(peakOccupancy, occupancyPct);
            if (occupancyPct > 90) hoursAbove90++;

            hours.add(new SimulatedHour(
                    bp.timestamp(),
                    bp.wardName(),
                    effectiveAdmissions,
                    bp.discharges(),
                    effectiveTotalBeds,
                    newOccupied,
                    bp.totalIcu(),
                    Math.min(bp.totalIcu(), bp.occupiedIcu() + (int)(effectiveAdmissions * 0.15)),
                    bp.totalVentilators(),
                    Math.min(bp.totalVentilators(), bp.usedVentilators() + (int)(effectiveAdmissions * 0.08)),
                    effectiveStaff,
                    bp.oxygenLevelPct(),
                    occupancyPct,
                    turnedAway
            ));
        }

        ScenarioKPIs kpis = new ScenarioKPIs(totalTurnedAway, peakOccupancy, hoursAbove90, duration);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(kpis, params);

        return new ScenarioResult(params, hours, kpis, recommendations);
    }

    private List<String> generateRecommendations(ScenarioKPIs kpis, ScenarioParams params) {
        List<String> recs = new ArrayList<>();

        if (kpis.peakOccupancyPct() > 95) {
            recs.add("🚨 CRITICAL: Peak occupancy exceeds 95%. Activate emergency overflow protocols immediately.");
        }
        if (kpis.hoursAbove90Pct() > kpis.totalHours() * 0.3) {
            recs.add("⚠️ Sustained high load: occupancy exceeds 90% for over 30% of the period. Consider diverting non-critical cases to nearby facilities.");
        }
        if (kpis.patientsTurnedAway() > 0) {
            recs.add("🏥 " + kpis.patientsTurnedAway() + " patients turned away. Add " +
                    Math.max(10, kpis.patientsTurnedAway() / 3) + " extra beds to eliminate this.");
        }
        if (params.triageMode().equals("NORMAL") && kpis.peakOccupancyPct() > 85) {
            recs.add("📋 Switch to MODERATE triage to reduce non-urgent admissions by ~15%.");
        }
        if (params.staffReassignment() == 0 && kpis.peakOccupancyPct() > 80) {
            recs.add("👩‍⚕️ Reassign 5-10 staff from low-load wards to handle surge capacity.");
        }
        if (params.extraBeds() == 0 && kpis.patientsTurnedAway() > 5) {
            recs.add("🛏️ Deploy temporary field beds (20-30 units) to absorb overflow.");
        }
        if (recs.isEmpty()) {
            recs.add("✅ Scenario within normal operating parameters. Continue monitoring.");
        }

        // Disclaimer
        recs.add("ℹ️ Statistical forecast, not clinical advice. Consult medical professionals for decisions.");

        return recs;
    }

    // ---- preset scenarios ----

    public static ScenarioParams monsoonFloodPreset() {
        return new ScenarioParams(3.5, "STRICT", 20, 8, 72, "Monsoon Flood");
    }

    public static ScenarioParams dengueOutbreakPreset() {
        return new ScenarioParams(2.5, "MODERATE", 15, 5, 48, "Dengue Outbreak");
    }

    public static ScenarioParams highwayPileUpPreset() {
        return new ScenarioParams(4.0, "STRICT", 10, 12, 24, "Highway Pile-up");
    }

    public static ScenarioParams normalPreset() {
        return new ScenarioParams(1.0, "NORMAL", 0, 0, 72, "Normal Operations");
    }
}
