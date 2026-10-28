package com.sanjeevanigrid.integration.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_feed_logs")
public class HospitalFeedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType; // API_KEY, CSV_IMPORT, SIMULATED

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType; // Location, Observation, Appointment, Patient, Bundle, CSV_BATCH

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUCCESS"; // SUCCESS, PARTIAL, ERROR

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public HospitalFeedLog() {}

    public HospitalFeedLog(UUID hospitalId, String sourceType, String resourceType,
                           String status, String summary, String payload) {
        this.hospitalId = hospitalId;
        this.sourceType = sourceType;
        this.resourceType = resourceType;
        this.status = status;
        this.summary = summary;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
