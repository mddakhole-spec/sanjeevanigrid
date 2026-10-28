package com.sanjeevanigrid.integration.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "licence_number", length = 100)
    private String licenceNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "district", length = 100)
    private String district = "Yavatmal";

    @Column(name = "state", length = 100)
    private String state = "Maharashtra";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "admin_user_id")
    private UUID adminUserId;

    @Column(name = "api_key_hash", length = 255)
    private String apiKeyHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Transient coordinates for easy access
    @Transient
    private Double latitude;

    @Transient
    private Double longitude;

    public Hospital() {}

    public Hospital(String name, String licenceNumber, String address, String district,
                    String phone, String email) {
        this.name = name;
        this.licenceNumber = licenceNumber;
        this.address = address;
        this.district = (district != null && !district.isBlank()) ? district : "Yavatmal";
        this.phone = phone;
        this.email = email;
        this.verified = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Double getLatitude() {
        if (latitude != null) return latitude;
        if ("Pusad Sub-District Hospital".equalsIgnoreCase(name)) return 19.9073;
        return 20.3888; // Default Yavatmal GH
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        if (longitude != null) return longitude;
        if ("Pusad Sub-District Hospital".equalsIgnoreCase(name)) return 77.5671;
        return 78.1204; // Default Yavatmal GH
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; this.updatedAt = Instant.now(); }
    public UUID getAdminUserId() { return adminUserId; }
    public void setAdminUserId(UUID adminUserId) { this.adminUserId = adminUserId; }
    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
