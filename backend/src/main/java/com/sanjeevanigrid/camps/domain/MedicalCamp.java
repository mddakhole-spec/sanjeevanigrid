package com.sanjeevanigrid.camps.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "medical_camps")
public class MedicalCamp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "organizer_name", nullable = false)
    private String organizerName = "District Health Administration";

    @Column(name = "camp_type", nullable = false, length = 50)
    private String campType = "GENERAL_HEALTH"; // GENERAL_HEALTH, VACCINATION, MATERNAL_CHILD, EYE_CARE, SCREENING

    @Column(name = "venue", nullable = false)
    private String venue;

    @Column(name = "village_id")
    private UUID villageId;

    @Column(name = "village_name")
    private String villageName;

    @Column(name = "district", nullable = false, length = 100)
    private String district = "Yavatmal";

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "camp_date", nullable = false)
    private LocalDate campDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime = LocalTime.of(9, 0);

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime = LocalTime.of(17, 0);

    @Column(name = "doctor_in_charge")
    private String doctorInCharge = "Dr. Nilesh Patil, MD";

    @Column(name = "target_attendees", nullable = false)
    private int targetAttendees = 150;

    @Column(name = "registered_count", nullable = false)
    private int registeredCount = 0;

    @Column(name = "attended_count", nullable = false)
    private int attendedCount = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SCHEDULED"; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "shareable_slug", length = 100, unique = true)
    private String shareableSlug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public MedicalCamp() {}

    public MedicalCamp(String name, String organizerName, String campType, String venue,
                       UUID villageId, String villageName, String district,
                       Double latitude, Double longitude, LocalDate campDate,
                       LocalTime startTime, LocalTime endTime, String doctorInCharge,
                       int targetAttendees) {
        this.name = name;
        this.organizerName = (organizerName != null && !organizerName.isBlank()) ? organizerName : "District Health Administration";
        this.campType = (campType != null && !campType.isBlank()) ? campType : "GENERAL_HEALTH";
        this.venue = venue;
        this.villageId = villageId;
        this.villageName = villageName;
        this.district = (district != null && !district.isBlank()) ? district : "Yavatmal";
        this.latitude = latitude;
        this.longitude = longitude;
        this.campDate = (campDate != null) ? campDate : LocalDate.now();
        this.startTime = (startTime != null) ? startTime : LocalTime.of(9, 0);
        this.endTime = (endTime != null) ? endTime : LocalTime.of(17, 0);
        this.doctorInCharge = doctorInCharge;
        this.targetAttendees = (targetAttendees > 0) ? targetAttendees : 150;
        this.status = "SCHEDULED";
        this.shareableSlug = generateSlug(name, campDate);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    private String generateSlug(String name, LocalDate date) {
        String clean = (name != null ? name : "camp").toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return clean + "-" + (date != null ? date.toString() : System.currentTimeMillis());
    }

    public void incrementRegistration() {
        this.registeredCount++;
        this.attendedCount++;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public String getCampType() { return campType; }
    public void setCampType(String campType) { this.campType = campType; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public UUID getVillageId() { return villageId; }
    public void setVillageId(UUID villageId) { this.villageId = villageId; }
    public String getVillageName() { return villageName; }
    public void setVillageName(String villageName) { this.villageName = villageName; }
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
    public String getDoctorInCharge() { return doctorInCharge; }
    public void setDoctorInCharge(String doctorInCharge) { this.doctorInCharge = doctorInCharge; }
    public int getTargetAttendees() { return targetAttendees; }
    public void setTargetAttendees(int targetAttendees) { this.targetAttendees = targetAttendees; }
    public int getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(int registeredCount) { this.registeredCount = registeredCount; }
    public int getAttendedCount() { return attendedCount; }
    public void setAttendedCount(int attendedCount) { this.attendedCount = attendedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public String getShareableSlug() { return shareableSlug; }
    public void setShareableSlug(String shareableSlug) { this.shareableSlug = shareableSlug; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

