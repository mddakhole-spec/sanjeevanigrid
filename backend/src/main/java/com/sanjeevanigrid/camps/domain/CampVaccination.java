package com.sanjeevanigrid.camps.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "camp_vaccinations")
public class CampVaccination {

    @Id
    private UUID id;

    @Column(name = "camp_id", nullable = false)
    private UUID campId;

    @Column(name = "attendee_id", nullable = false)
    private UUID attendeeId;

    @Column(name = "client_sync_id", nullable = false, unique = true)
    private UUID clientSyncId;

    @Column(name = "vaccine_name", nullable = false, length = 100)
    private String vaccineName;

    @Column(name = "dose_number", nullable = false)
    private int doseNumber = 1;

    @Column(name = "administered_date", nullable = false)
    private LocalDate administeredDate = LocalDate.now();

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "administered_by")
    private String administeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CampVaccination() {}

    public CampVaccination(UUID id, UUID campId, UUID attendeeId, UUID clientSyncId,
                           String vaccineName, int doseNumber, LocalDate administeredDate,
                           LocalDate nextDueDate, String batchNumber, String administeredBy) {
        this.id = (id != null) ? id : (clientSyncId != null ? clientSyncId : UUID.randomUUID());
        this.campId = campId;
        this.attendeeId = attendeeId;
        this.clientSyncId = (clientSyncId != null) ? clientSyncId : this.id;
        this.vaccineName = vaccineName;
        this.doseNumber = (doseNumber > 0) ? doseNumber : 1;
        this.administeredDate = (administeredDate != null) ? administeredDate : LocalDate.now();
        this.nextDueDate = nextDueDate;
        this.batchNumber = batchNumber;
        this.administeredBy = administeredBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCampId() { return campId; }
    public void setCampId(UUID campId) { this.campId = campId; }
    public UUID getAttendeeId() { return attendeeId; }
    public void setAttendeeId(UUID attendeeId) { this.attendeeId = attendeeId; }
    public UUID getClientSyncId() { return clientSyncId; }
    public void setClientSyncId(UUID clientSyncId) { this.clientSyncId = clientSyncId; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public int getDoseNumber() { return doseNumber; }
    public void setDoseNumber(int doseNumber) { this.doseNumber = doseNumber; }
    public LocalDate getAdministeredDate() { return administeredDate; }
    public void setAdministeredDate(LocalDate administeredDate) { this.administeredDate = administeredDate; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public String getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(String administeredBy) { this.administeredBy = administeredBy; }
    public Instant getCreatedAt() { return createdAt; }
}

