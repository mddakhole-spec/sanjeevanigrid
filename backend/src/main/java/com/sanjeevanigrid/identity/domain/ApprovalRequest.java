package com.sanjeevanigrid.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_queue")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "requested_role", nullable = false, length = 30)
    private String requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected ApprovalRequest() {}

    public static ApprovalRequest create(UUID userId, String requestedRole) {
        ApprovalRequest ar = new ApprovalRequest();
        ar.userId = userId;
        ar.requestedRole = requestedRole;
        return ar;
    }

    public void approve(UUID reviewerId, String note) {
        this.status = ApprovalStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void reject(UUID reviewerId, String note) {
        this.status = ApprovalStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getRequestedRole() { return requestedRole; }
    public ApprovalStatus getStatus() { return status; }
    public UUID getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
