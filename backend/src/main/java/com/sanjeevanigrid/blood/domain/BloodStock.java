package com.sanjeevanigrid.blood.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "blood_stocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hospital_id", "blood_group", "expiry_date"})
})
public class BloodStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @Column(name = "units_available", nullable = false)
    private int unitsAvailable;

    @Column(name = "units_reserved", nullable = false)
    private int unitsReserved = 0;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodStock() {}

    public BloodStock(UUID hospitalId, BloodGroup bloodGroup, int unitsAvailable, LocalDate expiryDate, int lowStockThreshold) {
        this.hospitalId = hospitalId;
        this.bloodGroup = bloodGroup;
        this.unitsAvailable = unitsAvailable;
        this.expiryDate = expiryDate;
        this.lowStockThreshold = lowStockThreshold;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isLowStock() {
        return unitsAvailable <= lowStockThreshold;
    }

    public boolean isExpiringSoon(int days) {
        if (expiryDate == null) return false;
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        return daysUntilExpiry >= 0 && daysUntilExpiry <= days;
    }

    public boolean hasAvailableUnits(int count) {
        return (unitsAvailable - unitsReserved) >= count;
    }

    public void deductUnits(int count) {
        if (count > unitsAvailable) {
            throw new IllegalStateException("Cannot deduct " + count + " units; available is " + unitsAvailable);
        }
        this.unitsAvailable -= count;
        this.updatedAt = Instant.now();
    }

    public void addUnits(int count) {
        this.unitsAvailable += count;
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public int getUnitsAvailable() { return unitsAvailable; }
    public void setUnitsAvailable(int unitsAvailable) { this.unitsAvailable = unitsAvailable; this.updatedAt = Instant.now(); }
    public int getUnitsReserved() { return unitsReserved; }
    public void setUnitsReserved(int unitsReserved) { this.unitsReserved = unitsReserved; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
