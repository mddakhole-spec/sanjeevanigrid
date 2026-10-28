package com.sanjeevanigrid.camps.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "camp_attendees")
public class CampAttendee {

    @Id
    private UUID id;

    @Column(name = "camp_id", nullable = false)
    private UUID campId;

    @Column(name = "client_sync_id", nullable = false, unique = true)
    private UUID clientSyncId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender = "OTHER"; // MALE, FEMALE, OTHER

    @Column(name = "qr_code_id", length = 100)
    private String qrCodeId;

    @Column(name = "village")
    private String village;

    @Column(name = "vital_bp", length = 20)
    private String vitalBp;

    @Column(name = "vital_pulse")
    private Integer vitalPulse;

    @Column(name = "vital_temp")
    private Double vitalTemp;

    @Column(name = "vital_spo2")
    private Integer vitalSpo2;

    @Column(name = "triage_level", nullable = false, length = 20)
    private String triageLevel = "ROUTINE"; // ROUTINE, URGENT, EMERGENCY

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "registered_offline", nullable = false)
    private boolean registeredOffline = false;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CampAttendee() {}

    public CampAttendee(UUID id, UUID campId, UUID clientSyncId, String fullName, String phone,
                        LocalDate dob, Integer age, String gender, String qrCodeId,
                        String village, String vitalBp, Integer vitalPulse, Double vitalTemp,
                        Integer vitalSpo2, String triageLevel, String symptoms, String notes,
                        boolean registeredOffline) {
        this.id = (id != null) ? id : (clientSyncId != null ? clientSyncId : UUID.randomUUID());
        this.campId = campId;
        this.clientSyncId = (clientSyncId != null) ? clientSyncId : this.id;
        this.fullName = fullName;
        this.phone = phone;
        this.dob = dob;
        this.age = age;
        this.gender = (gender != null && !gender.isBlank()) ? gender.toUpperCase() : "OTHER";
        this.qrCodeId = (qrCodeId != null && !qrCodeId.isBlank()) ? qrCodeId : "SG-CAMP-" + phone.replaceAll("[^0-9]", "");
        this.village = village;
        this.vitalBp = vitalBp;
        this.vitalPulse = vitalPulse;
        this.vitalTemp = vitalTemp;
        this.vitalSpo2 = vitalSpo2;
        this.triageLevel = (triageLevel != null && !triageLevel.isBlank()) ? triageLevel.toUpperCase() : "ROUTINE";
        this.symptoms = symptoms;
        this.notes = notes;
        this.registeredOffline = registeredOffline;
        this.syncedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCampId() { return campId; }
    public void setCampId(UUID campId) { this.campId = campId; }
    public UUID getClientSyncId() { return clientSyncId; }
    public void setClientSyncId(UUID clientSyncId) { this.clientSyncId = clientSyncId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getQrCodeId() { return qrCodeId; }
    public void setQrCodeId(String qrCodeId) { this.qrCodeId = qrCodeId; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getVitalBp() { return vitalBp; }
    public void setVitalBp(String vitalBp) { this.vitalBp = vitalBp; }
    public Integer getVitalPulse() { return vitalPulse; }
    public void setVitalPulse(Integer vitalPulse) { this.vitalPulse = vitalPulse; }
    public Double getVitalTemp() { return vitalTemp; }
    public void setVitalTemp(Double vitalTemp) { this.vitalTemp = vitalTemp; }
    public Integer getVitalSpo2() { return vitalSpo2; }
    public void setVitalSpo2(Integer vitalSpo2) { this.vitalSpo2 = vitalSpo2; }
    public String getTriageLevel() { return triageLevel; }
    public void setTriageLevel(String triageLevel) { this.triageLevel = triageLevel; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isRegisteredOffline() { return registeredOffline; }
    public void setRegisteredOffline(boolean registeredOffline) { this.registeredOffline = registeredOffline; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

