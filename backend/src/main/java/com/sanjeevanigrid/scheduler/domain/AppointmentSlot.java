package com.sanjeevanigrid.scheduler.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_slots", uniqueConstraints = {
    @UniqueConstraint(name = "uq_doctor_slot", columnNames = {"doctor_id", "slot_date", "start_time"})
})
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Column(name = "is_surge_reserved", nullable = false)
    private boolean isSurgeReserved = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AppointmentSlot() {}

    public AppointmentSlot(UUID doctorId, UUID hospitalId, LocalDate slotDate,
                           LocalTime startTime, LocalTime endTime) {
        this.doctorId = doctorId;
        this.hospitalId = hospitalId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = SlotStatus.AVAILABLE;
        this.isSurgeReserved = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isAvailable() {
        return this.status == SlotStatus.AVAILABLE && !this.isSurgeReserved;
    }

    public boolean isAvailableForEmergency() {
        return this.status == SlotStatus.AVAILABLE; // can claim even if surge reserved
    }

    public void book() {
        if (this.status == SlotStatus.BOOKED) {
            throw new IllegalStateException("Slot is already booked for date " + slotDate + " at " + startTime);
        }
        this.status = SlotStatus.BOOKED;
        this.updatedAt = Instant.now();
    }

    public void release() {
        this.status = isSurgeReserved ? SlotStatus.SURGE_RESERVED : SlotStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public void markSurgeReserved(boolean reserved) {
        this.isSurgeReserved = reserved;
        if (this.status == SlotStatus.AVAILABLE && reserved) {
            this.status = SlotStatus.SURGE_RESERVED;
        } else if (this.status == SlotStatus.SURGE_RESERVED && !reserved) {
            this.status = SlotStatus.AVAILABLE;
        }
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }
    public boolean isSurgeReserved() { return isSurgeReserved; }
    public void setSurgeReserved(boolean surgeReserved) { isSurgeReserved = surgeReserved; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
