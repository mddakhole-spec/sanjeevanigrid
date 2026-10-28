package com.sanjeevanigrid.camps.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Vaccine Schedule Engine implementing India's Universal Immunization Programme (UIP).
 * Evaluates patient Date of Birth (DOB) and past administered vaccination history,
 * determines compliance status (COMPLETED, DUE_NOW, OVERDUE, UPCOMING),
 * and computes exact next due dates.
 */
@Component
public class VaccineScheduleEngine {

    public record AdministeredDose(String vaccineName, int doseNumber, LocalDate administeredDate) {}

    public record VaccineStatus(
            String vaccineName,
            int doseNumber,
            String targetAgeDescription,
            LocalDate recommendedDate,
            String status, // COMPLETED, DUE_NOW, OVERDUE, UPCOMING
            LocalDate administeredDate,
            LocalDate nextDueDate,
            String instructions
    ) {}

    public record ScheduleResult(
            LocalDate dob,
            int ageInMonths,
            int totalVaccines,
            int completedCount,
            int dueNowCount,
            int overdueCount,
            int upcomingCount,
            List<VaccineStatus> schedule,
            List<VaccineStatus> actionableVaccines
    ) {}

    private static class Rule {
        final String name;
        final int dose;
        final int targetDays;
        final int graceDays;
        final String ageDesc;
        final String instructions;
        final String prereqVaccine;
        final int prereqDose;
        final int minDaysAfterPrereq;

        Rule(String name, int dose, int targetDays, int graceDays, String ageDesc, String instructions,
             String prereqVaccine, int prereqDose, int minDaysAfterPrereq) {
            this.name = name;
            this.dose = dose;
            this.targetDays = targetDays;
            this.graceDays = graceDays;
            this.ageDesc = ageDesc;
            this.instructions = instructions;
            this.prereqVaccine = prereqVaccine;
            this.prereqDose = prereqDose;
            this.minDaysAfterPrereq = minDaysAfterPrereq;
        }

        Rule(String name, int dose, int targetDays, int graceDays, String ageDesc, String instructions) {
            this(name, dose, targetDays, graceDays, ageDesc, instructions, null, 0, 0);
        }
    }

    private final List<Rule> rules = List.of(
            // At Birth
            new Rule("BCG", 1, 0, 90, "At Birth", "0.1ml intradermal on left upper arm"),
            new Rule("HepB_Birth", 1, 0, 1, "At Birth (within 24 hours)", "0.5ml intramuscular on anterolateral mid-thigh"),
            new Rule("OPV", 0, 0, 15, "At Birth (within 15 days)", "2 drops orally"),

            // 6 Weeks (42 days)
            new Rule("Pentavalent", 1, 42, 28, "6 Weeks", "0.5ml IM (DPT + HepB + Hib)"),
            new Rule("OPV", 1, 42, 28, "6 Weeks", "2 drops orally"),
            new Rule("Rotavirus", 1, 42, 28, "6 Weeks", "5 drops orally"),
            new Rule("PCV", 1, 42, 28, "6 Weeks", "0.5ml IM right thigh"),

            // 10 Weeks (70 days)
            new Rule("Pentavalent", 2, 70, 28, "10 Weeks", "0.5ml IM (DPT + HepB + Hib)", "Pentavalent", 1, 28),
            new Rule("OPV", 2, 70, 28, "10 Weeks", "2 drops orally", "OPV", 1, 28),
            new Rule("Rotavirus", 2, 70, 28, "10 Weeks", "5 drops orally", "Rotavirus", 1, 28),

            // 14 Weeks (98 days)
            new Rule("Pentavalent", 3, 98, 28, "14 Weeks", "0.5ml IM (DPT + HepB + Hib)", "Pentavalent", 2, 28),
            new Rule("OPV", 3, 98, 28, "14 Weeks", "2 drops orally", "OPV", 2, 28),
            new Rule("Rotavirus", 3, 98, 28, "14 Weeks", "5 drops orally", "Rotavirus", 2, 28),
            new Rule("PCV", 2, 98, 28, "14 Weeks", "0.5ml IM right thigh", "PCV", 1, 28),

            // 9-12 Months (270 days)
            new Rule("MR", 1, 270, 60, "9 - 12 Months", "0.5ml subcutaneous right upper arm (Measles & Rubella)"),
            new Rule("PCV_Booster", 1, 270, 60, "9 Months", "0.5ml IM right thigh", "PCV", 2, 60),
            new Rule("Vitamin_A", 1, 270, 60, "9 Months", "1ml (1 lakh IU) orally with MR-1"),

            // 16-24 Months (480 days)
            new Rule("MR", 2, 480, 90, "16 - 24 Months", "0.5ml subcutaneous right upper arm", "MR", 1, 180),
            new Rule("DPT_Booster", 1, 480, 90, "16 - 24 Months", "0.5ml IM mid-thigh/deltoid"),
            new Rule("OPV_Booster", 1, 480, 90, "16 - 24 Months", "2 drops orally"),

            // 5-6 Years (1825 days)
            new Rule("DPT_Booster", 2, 1825, 180, "5 - 6 Years", "0.5ml IM upper arm"),

            // 10 & 16 Years
            new Rule("Td", 1, 3650, 365, "10 Years", "0.5ml IM upper arm (Tetanus & Diphtheria)"),
            new Rule("Td", 2, 5840, 365, "16 Years", "0.5ml IM upper arm", "Td", 1, 365)
    );

    public ScheduleResult evaluateSchedule(LocalDate dob, List<AdministeredDose> history, LocalDate referenceDate) {
        if (dob == null) {
            dob = LocalDate.now();
        }
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        int ageInMonths = (int) ChronoUnit.MONTHS.between(dob, referenceDate);

        // Normalize administered doses by key "NAME:DOSE"
        Map<String, LocalDate> administeredMap = new HashMap<>();
        if (history != null) {
            for (AdministeredDose d : history) {
                if (d != null && d.vaccineName() != null) {
                    String key = normalizeVaccineName(d.vaccineName()) + ":" + d.doseNumber();
                    administeredMap.put(key, d.administeredDate() != null ? d.administeredDate() : referenceDate);
                }
            }
        }

        List<VaccineStatus> scheduleList = new ArrayList<>();
        List<VaccineStatus> actionableList = new ArrayList<>();
        int completed = 0;
        int dueNow = 0;
        int overdue = 0;
        int upcoming = 0;

        for (Rule rule : rules) {
            String key = normalizeVaccineName(rule.name) + ":" + rule.dose;
            LocalDate targetDate = dob.plusDays(rule.targetDays);
            LocalDate adminDate = administeredMap.get(key);

            if (adminDate != null) {
                // Completed
                completed++;
                scheduleList.add(new VaccineStatus(
                        rule.name, rule.dose, rule.ageDesc, targetDate,
                        "COMPLETED", adminDate, null, rule.instructions
                ));
            } else {
                // Not completed - check prerequisite dose if any
                LocalDate earliestAllowedDate = targetDate;
                if (rule.prereqVaccine != null) {
                    String prereqKey = normalizeVaccineName(rule.prereqVaccine) + ":" + rule.prereqDose;
                    LocalDate prereqDate = administeredMap.get(prereqKey);
                    if (prereqDate != null) {
                        LocalDate afterPrereq = prereqDate.plusDays(rule.minDaysAfterPrereq);
                        if (afterPrereq.isAfter(earliestAllowedDate)) {
                            earliestAllowedDate = afterPrereq;
                        }
                    }
                }

                String status;
                LocalDate nextDue;

                if (referenceDate.isBefore(earliestAllowedDate)) {
                    status = "UPCOMING";
                    nextDue = earliestAllowedDate;
                    upcoming++;
                } else {
                    LocalDate overdueDate = earliestAllowedDate.plusDays(rule.graceDays);
                    if (referenceDate.isAfter(overdueDate)) {
                        status = "OVERDUE";
                        overdue++;
                    } else {
                        status = "DUE_NOW";
                        dueNow++;
                    }
                    nextDue = referenceDate;
                }

                VaccineStatus vs = new VaccineStatus(
                        rule.name, rule.dose, rule.ageDesc, earliestAllowedDate,
                        status, null, nextDue, rule.instructions
                );
                scheduleList.add(vs);

                if ("DUE_NOW".equals(status) || "OVERDUE".equals(status)) {
                    actionableList.add(vs);
                }
            }
        }

        return new ScheduleResult(
                dob, ageInMonths, rules.size(), completed, dueNow, overdue, upcoming, scheduleList, actionableList
        );
    }

    public ScheduleResult evaluateSchedule(LocalDate dob, List<AdministeredDose> history) {
        return evaluateSchedule(dob, history, LocalDate.now());
    }

    private String normalizeVaccineName(String name) {
        if (name == null) return "";
        return name.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}

