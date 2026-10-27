package com.sanjeevanigrid.identity.infra;

import com.sanjeevanigrid.identity.domain.ApprovalRequest;
import com.sanjeevanigrid.identity.domain.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    List<ApprovalRequest> findByStatus(ApprovalStatus status);
    List<ApprovalRequest> findByUserId(UUID userId);
}
