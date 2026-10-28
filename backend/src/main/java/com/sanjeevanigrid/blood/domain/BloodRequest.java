package com.sanjeevanigrid.blood.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_phone", length = 20)
    private String patientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @Column(name = "units_needed", nullable = false)
    private int unitsNeeded = 1;

    @Column(name = "units_allocated_from_stock", nullable = false)
    private int unitsAllocatedFromStock = 0;

    @Column(name = "units_fulfilled_by_donors", nullable = false)
    private int unitsFulfilledByDonors = 0;

    @Column(name = "urgency", nullable = false, length = 30)
    private String urgency = "URGENT"; // NORMAL, URGENT, CRITICAL_EMERGENCY

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SEARCHING_STOCK"; // SEARCHING_STOCK, STOCK_ALLOCATED, ALERTING_DONORS, PARTIALLY_FULFILLED, FULFILLED, CANCELLED

    @Column(name = "fulfillment_source", length = 30)
    private String fulfillmentSource; // STOCK, DONORS, MIXED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodRequest() {}

    public BloodRequest(String requestNumber, UUID hospitalId, String patientName,
                        String patientPhone, BloodGroup bloodGroup, int unitsNeeded, String urgency) {
        this.requestNumber = requestNumber;
        this.hospitalId = hospitalId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.bloodGroup = bloodGroup;
        this.unitsNeeded = unitsNeeded;
        this.urgency = (urgency != null && !urgency.isBlank()) ? urgency : "URGENT";
        this.status = "SEARCHING_STOCK";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isFullyFulfilled() {
        return (unitsAllocatedFromStock + unitsFulfilledByDonors) >= unitsNeeded;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public int getUnitsNeeded() { return unitsNeeded; }
    public void setUnitsNeeded(int unitsNeeded) { this.unitsNeeded = unitsNeeded; }
    public int getUnitsAllocatedFromStock() { return unitsAllocatedFromStock; }
    public void setUnitsAllocatedFromStock(int unitsAllocatedFromStock) { this.unitsAllocatedFromStock = unitsAllocatedFromStock; this.updatedAt = Instant.now(); }
    public int getUnitsFulfilledByDonors() { return unitsFulfilledByDonors; }
    public void setUnitsFulfilledByDonors(int unitsFulfilledByDonors) { this.unitsFulfilledByDonors = unitsFulfilledByDonors; this.updatedAt = Instant.now(); }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public String getFulfillmentSource() { return fulfillmentSource; }
    public void setFulfillmentSource(String fulfillmentSource) { this.fulfillmentSource = fulfillmentSource; this.updatedAt = Instant.now(); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
