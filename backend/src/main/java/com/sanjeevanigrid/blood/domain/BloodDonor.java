package com.sanjeevanigrid.blood.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "blood_donors")
public class BloodDonor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "district", nullable = false)
    private String district = "Yavatmal";

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven = true;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "total_alerts_received", nullable = false)
    private int totalAlertsReceived = 0;

    @Column(name = "total_alerts_responded", nullable = false)
    private int totalAlertsResponded = 0;

    @Column(name = "total_donations_fulfilled", nullable = false)
    private int totalDonationsFulfilled = 0;

    @Column(name = "last_response_at")
    private Instant lastResponseAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodDonor() {}

    public BloodDonor(String fullName, String phone, BloodGroup bloodGroup,
                      double latitude, double longitude, String address, String district) {
        this.fullName = fullName;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.district = (district != null && !district.isBlank()) ? district : "Yavatmal";
        this.available = true;
        this.consentGiven = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordAlertSent() {
        this.totalAlertsReceived++;
        this.updatedAt = Instant.now();
    }

    public void recordResponse(boolean fulfilled) {
        this.totalAlertsResponded++;
        this.lastResponseAt = Instant.now();
        if (fulfilled) {
            this.totalDonationsFulfilled++;
            this.lastDonationDate = LocalDate.now();
        }
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; this.updatedAt = Instant.now(); }
    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; this.updatedAt = Instant.now(); }
    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public int getTotalAlertsReceived() { return totalAlertsReceived; }
    public void setTotalAlertsReceived(int totalAlertsReceived) { this.totalAlertsReceived = totalAlertsReceived; }
    public int getTotalAlertsResponded() { return totalAlertsResponded; }
    public void setTotalAlertsResponded(int totalAlertsResponded) { this.totalAlertsResponded = totalAlertsResponded; }
    public int getTotalDonationsFulfilled() { return totalDonationsFulfilled; }
    public void setTotalDonationsFulfilled(int totalDonationsFulfilled) { this.totalDonationsFulfilled = totalDonationsFulfilled; }
    public Instant getLastResponseAt() { return lastResponseAt; }
    public void setLastResponseAt(Instant lastResponseAt) { this.lastResponseAt = lastResponseAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
