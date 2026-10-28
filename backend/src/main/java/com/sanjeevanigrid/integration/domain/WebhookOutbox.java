package com.sanjeevanigrid.integration.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_outbox")
public class WebhookOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, DELIVERED, FAILED

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WebhookOutbox() {}

    public WebhookOutbox(UUID hospitalId, String eventType, String payload, String targetUrl) {
        this.hospitalId = hospitalId;
        this.eventType = eventType;
        this.payload = payload;
        this.targetUrl = targetUrl;
        this.status = "PENDING";
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public void markDelivered() {
        this.status = "DELIVERED";
        this.attempts++;
        this.lastAttemptAt = Instant.now();
    }

    public void markFailed() {
        this.attempts++;
        this.lastAttemptAt = Instant.now();
        this.status = (this.attempts >= 3) ? "FAILED" : "PENDING";
        this.nextRetryAt = Instant.now().plusSeconds(60L * this.attempts);
    }

    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public int getAttempts() { return attempts; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getCreatedAt() { return createdAt; }
}
