package com.sanjeevanigrid.camps.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "camp_services_log")
public class CampServiceLog {

    @Id
    private UUID id;

    @Column(name = "camp_id", nullable = false)
    private UUID campId;

    @Column(name = "attendee_id", nullable = false)
    private UUID attendeeId;

    @Column(name = "client_sync_id", nullable = false, unique = true)
    private UUID clientSyncId;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType; // CONSULTATION, VACCINATION, LAB_TEST, MEDICINE_DISPENSE, REFERRAL

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CampServiceLog() {}

    public CampServiceLog(UUID id, UUID campId, UUID attendeeId, UUID clientSyncId,
                          String serviceType, String details, String doctorNotes, String performedBy) {
        this.id = (id != null) ? id : (clientSyncId != null ? clientSyncId : UUID.randomUUID());
        this.campId = campId;
        this.attendeeId = attendeeId;
        this.clientSyncId = (clientSyncId != null) ? clientSyncId : this.id;
        this.serviceType = serviceType;
        this.details = details;
        this.doctorNotes = doctorNotes;
        this.performedBy = performedBy;
        this.performedAt = Instant.now();
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
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }
    public Instant getCreatedAt() { return createdAt; }
}

