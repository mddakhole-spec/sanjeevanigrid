package com.sanjeevanigrid.scheduler.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "appointment_waitlist")
public class AppointmentWaitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_phone", nullable = false, length = 20)
    private String patientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private AppointmentPriority priority = AppointmentPriority.ROUTINE;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "WAITING"; // WAITING, PROMOTED, EXPIRED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "promoted_at")
    private Instant promotedAt;

    protected AppointmentWaitlist() {}

    public AppointmentWaitlist(UUID doctorId, LocalDate appointmentDate, UUID patientId,
                               String patientName, String patientPhone, AppointmentPriority priority) {
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.priority = priority != null ? priority : AppointmentPriority.ROUTINE;
        this.status = "WAITING";
        this.createdAt = Instant.now();
    }

    public void promote() {
        this.status = "PROMOTED";
        this.promotedAt = Instant.now();
    }

    public void expire() {
        this.status = "EXPIRED";
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public AppointmentPriority getPriority() { return priority; }
    public void setPriority(AppointmentPriority priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPromotedAt() { return promotedAt; }
}
