package com.sanjeevanigrid.blood.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blood_donor_alerts")
public class BloodDonorAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blood_request_id", nullable = false)
    private UUID bloodRequestId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel = "WHATSAPP";

    @Column(name = "alert_order", nullable = false)
    private int alertOrder;

    @Column(name = "reliability_score", nullable = false)
    private double reliabilityScore;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SENT"; // SENT, ACCEPTED, DECLINED, EXPIRED

    @Column(name = "whatsapp_link", columnDefinition = "TEXT")
    private String whatsappLink;

    @Column(name = "telegram_status", length = 30)
    private String telegramStatus;

    @Column(name = "sms_status", length = 30)
    private String smsStatus;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    public BloodDonorAlert() {}

    public BloodDonorAlert(UUID bloodRequestId, UUID donorId, String channel, int alertOrder,
                           double reliabilityScore, double distanceKm, String whatsappLink) {
        this.bloodRequestId = bloodRequestId;
        this.donorId = donorId;
        this.channel = channel;
        this.alertOrder = alertOrder;
        this.reliabilityScore = reliabilityScore;
        this.distanceKm = distanceKm;
        this.whatsappLink = whatsappLink;
        this.status = "SENT";
        this.sentAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public UUID getBloodRequestId() { return bloodRequestId; }
    public void setBloodRequestId(UUID bloodRequestId) { this.bloodRequestId = bloodRequestId; }
    public UUID getDonorId() { return donorId; }
    public void setDonorId(UUID donorId) { this.donorId = donorId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public int getAlertOrder() { return alertOrder; }
    public void setAlertOrder(int alertOrder) { this.alertOrder = alertOrder; }
    public double getReliabilityScore() { return reliabilityScore; }
    public void setReliabilityScore(double reliabilityScore) { this.reliabilityScore = reliabilityScore; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWhatsappLink() { return whatsappLink; }
    public void setWhatsappLink(String whatsappLink) { this.whatsappLink = whatsappLink; }
    public String getTelegramStatus() { return telegramStatus; }
    public void setTelegramStatus(String telegramStatus) { this.telegramStatus = telegramStatus; }
    public String getSmsStatus() { return smsStatus; }
    public void setSmsStatus(String smsStatus) { this.smsStatus = smsStatus; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
}
