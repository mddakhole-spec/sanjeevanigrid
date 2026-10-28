package com.sanjeevanigrid.equity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "villages")
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "district", nullable = false)
    private String district = "Yavatmal";

    @Column(name = "taluka", nullable = false)
    private String taluka;

    @Column(name = "population", nullable = false)
    private int population;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "nearest_hospital_id")
    private UUID nearestHospitalId;

    @Column(name = "nearest_hospital_name")
    private String nearestHospitalName;

    @Column(name = "distance_to_hospital_km", nullable = false)
    private double distanceToHospitalKm;

    @Column(name = "travel_time_minutes", nullable = false)
    private int travelTimeMinutes;

    @Column(name = "doctor_availability_score", nullable = false)
    private double doctorAvailabilityScore = 0.5;

    @Column(name = "last_camp_date")
    private LocalDate lastCampDate;

    @Column(name = "days_since_last_camp", nullable = false)
    private int daysSinceLastCamp = 180;

    @Column(name = "access_score", nullable = false)
    private double accessScore = 45.0;

    @Column(name = "vulnerability_category", nullable = false, length = 20)
    private String vulnerabilityCategory = "YELLOW"; // RED, YELLOW, GREEN

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Village() {}

    public Village(String name, String district, String taluka, int population,
                   Double latitude, Double longitude, String nearestHospitalName,
                   double distanceToHospitalKm, int travelTimeMinutes,
                   double doctorAvailabilityScore, LocalDate lastCampDate) {
        this.name = name;
        this.district = (district != null) ? district : "Yavatmal";
        this.taluka = taluka;
        this.population = population;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nearestHospitalName = nearestHospitalName;
        this.distanceToHospitalKm = distanceToHospitalKm;
        this.travelTimeMinutes = travelTimeMinutes;
        this.doctorAvailabilityScore = doctorAvailabilityScore;
        this.lastCampDate = lastCampDate;
        if (lastCampDate != null) {
            this.daysSinceLastCamp = (int) ChronoUnit.DAYS.between(lastCampDate, LocalDate.now());
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateCampDate(LocalDate campDate) {
        this.lastCampDate = campDate;
        this.daysSinceLastCamp = (int) ChronoUnit.DAYS.between(campDate, LocalDate.now());
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getTaluka() { return taluka; }
    public void setTaluka(String taluka) { this.taluka = taluka; }
    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public UUID getNearestHospitalId() { return nearestHospitalId; }
    public void setNearestHospitalId(UUID nearestHospitalId) { this.nearestHospitalId = nearestHospitalId; }
    public String getNearestHospitalName() { return nearestHospitalName; }
    public void setNearestHospitalName(String nearestHospitalName) { this.nearestHospitalName = nearestHospitalName; }
    public double getDistanceToHospitalKm() { return distanceToHospitalKm; }
    public void setDistanceToHospitalKm(double distanceToHospitalKm) { this.distanceToHospitalKm = distanceToHospitalKm; }
    public int getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(int travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }
    public double getDoctorAvailabilityScore() { return doctorAvailabilityScore; }
    public void setDoctorAvailabilityScore(double doctorAvailabilityScore) { this.doctorAvailabilityScore = doctorAvailabilityScore; }
    public LocalDate getLastCampDate() { return lastCampDate; }
    public void setLastCampDate(LocalDate lastCampDate) { this.lastCampDate = lastCampDate; }
    public int getDaysSinceLastCamp() { return daysSinceLastCamp; }
    public void setDaysSinceLastCamp(int daysSinceLastCamp) { this.daysSinceLastCamp = daysSinceLastCamp; }
    public double getAccessScore() { return accessScore; }
    public void setAccessScore(double accessScore) { this.accessScore = accessScore; }
    public String getVulnerabilityCategory() { return vulnerabilityCategory; }
    public void setVulnerabilityCategory(String vulnerabilityCategory) { this.vulnerabilityCategory = vulnerabilityCategory; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

