package com.sanjeevanigrid.scheduler.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slot_id")
    private UUID slotId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_phone", nullable = false, length = 20)
    private String patientPhone;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Column(name = "patient_gender", length = 10)
    private String patientGender;

    @Column(name = "chief_complaint")
    private String chiefComplaint;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private AppointmentPriority priority = AppointmentPriority.ROUTINE;

    @Column(name = "priority_score", nullable = false)
    private int priorityScore = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "token_number", nullable = false, length = 30)
    private String tokenNumber;

    @Column(name = "queue_position", nullable = false)
    private int queuePosition = 0;

    @Column(name = "estimated_wait_mins", nullable = false)
    private int estimatedWaitMins = 0;

    @Column(name = "is_walk_in", nullable = false)
    private boolean isWalkIn = false;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "bumped_reason")
    private String bumpedReason;

    @Column(name = "bumped_at")
    private Instant bumpedAt;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent = false;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "consultation_start_at")
    private Instant consultationStartAt;

    @Column(name = "consultation_end_at")
    private Instant consultationEndAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Appointment() {}

    public Appointment(UUID slotId, UUID doctorId, UUID hospitalId, UUID patientId,
                       String patientName, String patientPhone, Integer patientAge,
                       String patientGender, String chiefComplaint, AppointmentPriority priority,
                       String tokenNumber, LocalDate appointmentDate, LocalTime startTime,
                       LocalTime endTime, boolean isWalkIn, String idempotencyKey) {
        this.slotId = slotId;
        this.doctorId = doctorId;
        this.hospitalId = hospitalId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.patientAge = patientAge;
        this.patientGender = patientGender;
        this.chiefComplaint = chiefComplaint;
        this.priority = priority != null ? priority : AppointmentPriority.ROUTINE;
        this.priorityScore = this.priority.getLevel();
        this.status = AppointmentStatus.CONFIRMED;
        this.tokenNumber = tokenNumber;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isWalkIn = isWalkIn;
        this.idempotencyKey = idempotencyKey;
        this.reminderSent = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void bump(String reason) {
        this.status = AppointmentStatus.BUMPED;
        this.bumpedReason = reason;
        this.bumpedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void checkIn() {
        this.status = AppointmentStatus.CHECKED_IN;
        this.checkedInAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void startConsultation() {
        this.status = AppointmentStatus.IN_CONSULTATION;
        this.consultationStartAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public double completeConsultation() {
        this.status = AppointmentStatus.COMPLETED;
        this.consultationEndAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.consultationStartAt != null) {
            long durationMillis = java.time.Duration.between(consultationStartAt, consultationEndAt).toMillis();
            return Math.max(1.0, durationMillis / (60.0 * 1000.0));
        }
        return 15.0; // default estimated duration
    }

    public void markNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSlotId() { return slotId; }
    public void setSlotId(UUID slotId) { this.slotId = slotId; }
    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }
    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public AppointmentPriority getPriority() { return priority; }
    public void setPriority(AppointmentPriority priority) {
        this.priority = priority;
        this.priorityScore = priority != null ? priority.getLevel() : 1;
    }
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }
    public int getEstimatedWaitMins() { return estimatedWaitMins; }
    public void setEstimatedWaitMins(int estimatedWaitMins) { this.estimatedWaitMins = estimatedWaitMins; }
    public boolean isWalkIn() { return isWalkIn; }
    public void setWalkIn(boolean walkIn) { isWalkIn = walkIn; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getBumpedReason() { return bumpedReason; }
    public void setBumpedReason(String bumpedReason) { this.bumpedReason = bumpedReason; }
    public Instant getBumpedAt() { return bumpedAt; }
    public void setBumpedAt(Instant bumpedAt) { this.bumpedAt = bumpedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Instant checkedInAt) { this.checkedInAt = checkedInAt; }
    public Instant getConsultationStartAt() { return consultationStartAt; }
    public void setConsultationStartAt(Instant consultationStartAt) { this.consultationStartAt = consultationStartAt; }
    public Instant getConsultationEndAt() { return consultationEndAt; }
    public void setConsultationEndAt(Instant consultationEndAt) { this.consultationEndAt = consultationEndAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
