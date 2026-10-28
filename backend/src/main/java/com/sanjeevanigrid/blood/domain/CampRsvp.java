package com.sanjeevanigrid.blood.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "camp_rsvps", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"camp_id", "donor_phone"})
})
public class CampRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "camp_id", nullable = false)
    private UUID campId;

    @Column(name = "donor_name", nullable = false)
    private String donorName;

    @Column(name = "donor_phone", nullable = false, length = 20)
    private String donorPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CampRsvp() {}

    public CampRsvp(UUID campId, String donorName, String donorPhone, BloodGroup bloodGroup, String notes) {
        this.campId = campId;
        this.donorName = donorName;
        this.donorPhone = donorPhone;
        this.bloodGroup = bloodGroup;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCampId() { return campId; }
    public void setCampId(UUID campId) { this.campId = campId; }
    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    public String getDonorPhone() { return donorPhone; }
    public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
