package com.sanjeevanigrid.camps.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "camp_medicine_stock")
public class CampMedicineStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "camp_id", nullable = false)
    private UUID campId;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit = "tablets"; // tablets, capsules, sachets, bottles, strips

    @Column(name = "initial_quantity", nullable = false)
    private int initialQuantity = 0;

    @Column(name = "dispensed_quantity", nullable = false)
    private int dispensedQuantity = 0;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CampMedicineStock() {}

    public CampMedicineStock(UUID campId, String medicineName, String unit, int initialQuantity) {
        this.campId = campId;
        this.medicineName = medicineName;
        this.unit = (unit != null && !unit.isBlank()) ? unit : "tablets";
        this.initialQuantity = initialQuantity;
        this.dispensedQuantity = 0;
        this.remainingQuantity = initialQuantity;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean dispense(int count) {
        if (count <= 0 || remainingQuantity < count) {
            return false;
        }
        this.dispensedQuantity += count;
        this.remainingQuantity -= count;
        this.updatedAt = Instant.now();
        return true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCampId() { return campId; }
    public void setCampId(UUID campId) { this.campId = campId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(int initialQuantity) { this.initialQuantity = initialQuantity; }
    public int getDispensedQuantity() { return dispensedQuantity; }
    public void setDispensedQuantity(int dispensedQuantity) { this.dispensedQuantity = dispensedQuantity; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

