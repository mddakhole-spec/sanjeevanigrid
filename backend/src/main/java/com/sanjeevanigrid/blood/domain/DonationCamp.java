package com.sanjeevanigrid.blood.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "donation_camps")
public class DonationCamp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "organizer_name", nullable = false)
    private String organizerName = "District Red Cross Society";

    @Column(name = "venue", nullable = false)
    private String venue;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "district", nullable = false)
    private String district = "Yavatmal";

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "camp_date", nullable = false)
    private LocalDate campDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "target_units", nullable = false)
    private int targetUnits = 50;

    @Column(name = "registered_count", nullable = false)
    private int registeredCount = 0;

    @Column(name = "collected_units", nullable = false)
    private int collectedUnits = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SCHEDULED"; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DonationCamp() {}

    public DonationCamp(String name, String organizerName, String venue, String address,
                        String district, Double latitude, Double longitude,
                        LocalDate campDate, LocalTime startTime, LocalTime endTime, int targetUnits) {
        this.name = name;
        this.organizerName = (organizerName != null && !organizerName.isBlank()) ? organizerName : "District Red Cross Society";
        this.venue = venue;
        this.address = address;
        this.district = (district != null && !district.isBlank()) ? district : "Yavatmal";
        this.latitude = latitude;
        this.longitude = longitude;
        this.campDate = campDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.targetUnits = targetUnits;
        this.status = "SCHEDULED";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void incrementRsvp() {
        this.registeredCount++;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDate getCampDate() { return campDate; }
    public void setCampDate(LocalDate campDate) { this.campDate = campDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public int getTargetUnits() { return targetUnits; }
    public void setTargetUnits(int targetUnits) { this.targetUnits = targetUnits; }
    public int getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }
    public int getCollectedUnits() { return collectedUnits; }
    public void setCollectedUnits(int collectedUnits) { this.collectedUnits = collectedUnits; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
