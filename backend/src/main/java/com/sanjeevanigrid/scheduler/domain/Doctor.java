package com.sanjeevanigrid.scheduler.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false, length = 100)
    private Specialty specialty;

    @Column(name = "qualification")
    private String qualification = "MBBS, MD";

    @Column(name = "experience_years")
    private Integer experienceYears = 5;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "consultation_duration_mins", nullable = false)
    private int consultationDurationMins = 15;

    @Column(name = "max_daily_patients", nullable = false)
    private int maxDailyPatients = 24;

    @Column(name = "rolling_avg_consultation_mins", nullable = false)
    private double rollingAvgConsultationMins = 15.0;

    @Column(name = "completed_consultations_count", nullable = false)
    private int completedConsultationsCount = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Doctor() {}

    public Doctor(UUID id, UUID hospitalId, String fullName, Specialty specialty,
                  String qualification, Integer experienceYears, BigDecimal consultationFee,
                  int consultationDurationMins, int maxDailyPatients) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.fullName = fullName;
        this.specialty = specialty;
        this.qualification = qualification;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
        this.consultationDurationMins = consultationDurationMins;
        this.maxDailyPatients = maxDailyPatients;
        this.rollingAvgConsultationMins = (double) consultationDurationMins;
        this.completedConsultationsCount = 0;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordConsultationDuration(double actualDurationMins) {
        if (actualDurationMins <= 0) actualDurationMins = this.consultationDurationMins;
        this.rollingAvgConsultationMins = (this.rollingAvgConsultationMins * this.completedConsultationsCount + actualDurationMins)
                / (this.completedConsultationsCount + 1);
        this.completedConsultationsCount++;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
    public int getConsultationDurationMins() { return consultationDurationMins; }
    public void setConsultationDurationMins(int consultationDurationMins) { this.consultationDurationMins = consultationDurationMins; }
    public int getMaxDailyPatients() { return maxDailyPatients; }
    public void setMaxDailyPatients(int maxDailyPatients) { this.maxDailyPatients = maxDailyPatients; }
    public double getRollingAvgConsultationMins() { return rollingAvgConsultationMins; }
    public void setRollingAvgConsultationMins(double rollingAvgConsultationMins) { this.rollingAvgConsultationMins = rollingAvgConsultationMins; }
    public int getCompletedConsultationsCount() { return completedConsultationsCount; }
    public void setCompletedConsultationsCount(int completedConsultationsCount) { this.completedConsultationsCount = completedConsultationsCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
