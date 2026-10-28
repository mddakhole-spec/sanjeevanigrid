package com.sanjeevanigrid.camps.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CampSyncDto {

    public record SyncRegistration(
            UUID id,
            UUID clientSyncId,
            String fullName,
            String phone,
            LocalDate dob,
            Integer age,
            String gender,
            String qrCodeId,
            String village,
            String vitalBp,
            Integer vitalPulse,
            Double vitalTemp,
            Integer vitalSpo2,
            String triageLevel,
            String symptoms,
            String notes,
            boolean registeredOffline,
            Instant clientTimestamp
    ) {}

    public record SyncService(
            UUID id,
            UUID clientSyncId,
            UUID attendeeId,
            UUID attendeeClientSyncId,
            String serviceType,
            String details,
            String doctorNotes,
            String performedBy
    ) {}

    public record SyncVaccination(
            UUID id,
            UUID clientSyncId,
            UUID attendeeId,
            UUID attendeeClientSyncId,
            String vaccineName,
            int doseNumber,
            LocalDate administeredDate,
            LocalDate nextDueDate,
            String batchNumber,
            String administeredBy
    ) {}

    public record SyncMedicineDispense(
            String medicineName,
            int quantity
    ) {}

    public record SyncBatch(
            UUID campId,
            List<SyncRegistration> registrations,
            List<SyncService> services,
            List<SyncVaccination> vaccinations,
            List<SyncMedicineDispense> dispenses
    ) {}

    public record SyncResult(
            String status,
            UUID campId,
            int syncedRegistrations,
            int duplicateRegistrations,
            int syncedServices,
            int syncedVaccinations,
            int totalCampAttendees,
            Instant serverTimestamp
    ) {}
}

