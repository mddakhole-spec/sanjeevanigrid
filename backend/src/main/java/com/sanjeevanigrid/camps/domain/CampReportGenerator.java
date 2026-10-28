package com.sanjeevanigrid.camps.domain;

import com.sanjeevanigrid.camps.infra.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CampReportGenerator {

    private final MedicalCampRepository campRepository;
    private final CampAttendeeRepository attendeeRepository;
    private final CampVaccinationRepository vaccinationRepository;
    private final CampServiceLogRepository serviceLogRepository;
    private final CampMedicineStockRepository stockRepository;

    public CampReportGenerator(MedicalCampRepository campRepository,
                               CampAttendeeRepository attendeeRepository,
                               CampVaccinationRepository vaccinationRepository,
                               CampServiceLogRepository serviceLogRepository,
                               CampMedicineStockRepository stockRepository) {
        this.campRepository = campRepository;
        this.attendeeRepository = attendeeRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.serviceLogRepository = serviceLogRepository;
        this.stockRepository = stockRepository;
    }

    public record CampReportDto(
            MedicalCamp camp,
            int totalRegistered,
            int totalAttended,
            Map<String, Long> triageBreakdown,
            Map<String, Integer> vitalsAlerts,
            Map<String, Long> demographics,
            int totalVaccinationsAdministered,
            Map<String, Long> vaccineBreakdown,
            List<CampMedicineStock> medicineStock,
            int totalServicesProvided,
            Instant generatedAt
    ) {}

    public CampReportDto generateReport(UUID campId) {
        MedicalCamp camp = campRepository.findById(campId)
                .orElseThrow(() -> new IllegalArgumentException("Camp not found: " + campId));

        List<CampAttendee> attendees = attendeeRepository.findByCampIdOrderByCreatedAtDesc(campId);
        List<CampVaccination> vaccinations = vaccinationRepository.findByCampId(campId);
        List<CampServiceLog> services = serviceLogRepository.findByCampId(campId);
        List<CampMedicineStock> stocks = stockRepository.findByCampId(campId);

        // Triage breakdown
        Map<String, Long> triageBreakdown = attendees.stream()
                .collect(Collectors.groupingBy(CampAttendee::getTriageLevel, Collectors.counting()));

        // Vitals alerts
        int highBp = 0;
        int fever = 0;
        int lowSpo2 = 0;

        for (CampAttendee a : attendees) {
            if (a.getVitalBp() != null && a.getVitalBp().contains("/")) {
                try {
                    String[] parts = a.getVitalBp().split("/");
                    int sbp = Integer.parseInt(parts[0].trim());
                    int dbp = Integer.parseInt(parts[1].trim());
                    if (sbp >= 140 || dbp >= 90) highBp++;
                } catch (Exception ignored) {}
            }
            if (a.getVitalTemp() != null && a.getVitalTemp() >= 100.4) fever++;
            if (a.getVitalSpo2() != null && a.getVitalSpo2() < 95) lowSpo2++;
        }

        Map<String, Integer> vitalsAlerts = Map.of(
                "highBpCount", highBp,
                "feverCount", fever,
                "lowSpo2Count", lowSpo2
        );

        // Demographics
        long children = attendees.stream().filter(a -> a.getAge() != null && a.getAge() < 12).count();
        long adults = attendees.stream().filter(a -> a.getAge() != null && a.getAge() >= 12 && a.getAge() < 60).count();
        long elderly = attendees.stream().filter(a -> a.getAge() != null && a.getAge() >= 60).count();
        long males = attendees.stream().filter(a -> "MALE".equalsIgnoreCase(a.getGender())).count();
        long females = attendees.stream().filter(a -> "FEMALE".equalsIgnoreCase(a.getGender())).count();
        long others = attendees.size() - males - females;

        Map<String, Long> demographics = Map.of(
                "childrenUnder12", children,
                "adults12To59", adults,
                "elderly60Plus", elderly,
                "males", males,
                "females", females,
                "others", others
        );

        // Vaccine breakdown
        Map<String, Long> vaccineBreakdown = vaccinations.stream()
                .collect(Collectors.groupingBy(CampVaccination::getVaccineName, Collectors.counting()));

        return new CampReportDto(
                camp,
                attendees.size(),
                attendees.size(),
                triageBreakdown,
                vitalsAlerts,
                demographics,
                vaccinations.size(),
                vaccineBreakdown,
                stocks,
                services.size(),
                Instant.now()
        );
    }

    public String generateCsv(UUID campId) {
        MedicalCamp camp = campRepository.findById(campId)
                .orElseThrow(() -> new IllegalArgumentException("Camp not found: " + campId));

        List<CampAttendee> attendees = attendeeRepository.findByCampIdOrderByCreatedAtDesc(campId);

        StringBuilder sb = new StringBuilder();
        sb.append("Camp: ").append(camp.getName()).append(" (").append(camp.getCampDate()).append(")\n");
        sb.append("Attendee ID,Client Sync ID,Full Name,Phone,Age,Gender,Village,Triage Level,Blood Pressure,Pulse,Temperature,SpO2,Symptoms,Doctor Notes,Registered Offline,Synced At\n");

        for (CampAttendee a : attendees) {
            sb.append(escape(a.getId())).append(",")
                    .append(escape(a.getClientSyncId())).append(",")
                    .append(escape(a.getFullName())).append(",")
                    .append(escape(a.getPhone())).append(",")
                    .append(a.getAge() != null ? a.getAge() : "").append(",")
                    .append(escape(a.getGender())).append(",")
                    .append(escape(a.getVillage())).append(",")
                    .append(escape(a.getTriageLevel())).append(",")
                    .append(escape(a.getVitalBp())).append(",")
                    .append(a.getVitalPulse() != null ? a.getVitalPulse() : "").append(",")
                    .append(a.getVitalTemp() != null ? a.getVitalTemp() : "").append(",")
                    .append(a.getVitalSpo2() != null ? a.getVitalSpo2() : "").append(",")
                    .append(escape(a.getSymptoms())).append(",")
                    .append(escape(a.getNotes())).append(",")
                    .append(a.isRegisteredOffline()).append(",")
                    .append(a.getSyncedAt() != null ? a.getSyncedAt().toString() : "")
                    .append("\n");
        }

        return sb.toString();
    }

    private String escape(Object val) {
        if (val == null) return "\"\"";
        String str = val.toString().replace("\"", "\"\"");
        return "\"" + str + "\"";
    }
}

