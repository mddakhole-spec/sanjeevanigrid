package com.sanjeevanigrid.identity.domain;

import com.sanjeevanigrid.identity.infra.ApprovalRequestRepository;
import com.sanjeevanigrid.identity.infra.RoleRepository;
import com.sanjeevanigrid.identity.infra.UserRepository;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin approval queue service.
 * Handles approval/rejection of user registrations for restricted roles.
 */
@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final NotificationGateway notificationGateway;

    public ApprovalService(ApprovalRequestRepository approvalRepo,
                           UserRepository userRepo,
                           RoleRepository roleRepo,
                           NotificationGateway notificationGateway) {
        this.approvalRepo = approvalRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.notificationGateway = notificationGateway;
    }

    /**
     * Submit a request for role approval (used during registration for non-CITIZEN roles).
     */
    @Transactional
    public ApprovalRequest submitForApproval(UUID userId, String requestedRole) {
        ApprovalRequest request = ApprovalRequest.create(userId, requestedRole);
        return approvalRepo.save(request);
    }

    /**
     * Get all pending approval requests (for admin dashboard).
     */
    public List<ApprovalRequest> getPendingRequests() {
        return approvalRepo.findByStatus(ApprovalStatus.PENDING);
    }

    /**
     * Approve a user's role request. Transitions user to ACTIVE, grants the role.
     */
    @Transactional
    public void approve(UUID requestId, UUID reviewerId, String note) {
        ApprovalRequest request = approvalRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found."));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Request already processed.");
        }

        request.approve(reviewerId, note);
        approvalRepo.save(request);

        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Grant requested role
        Role role = roleRepo.findByName(request.getRequestedRole())
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));
        user.addRole(role);

        // Transition to ACTIVE if in VERIFIED state
        if (user.getOnboardingState() == OnboardingState.VERIFIED) {
            user.transitionTo(OnboardingState.ACTIVE);
        }
        userRepo.save(user);

        // Notify user
        notificationGateway.sendInApp(user.getId(),
                "Account Approved",
                "Your " + request.getRequestedRole() + " role has been approved. You now have full access.");
    }

    /**
     * Reject a user's role request.
     */
    @Transactional
    public void reject(UUID requestId, UUID reviewerId, String note) {
        ApprovalRequest request = approvalRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found."));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Request already processed.");
        }

        request.reject(reviewerId, note);
        approvalRepo.save(request);

        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Transition to REJECTED if in VERIFIED state
        if (user.getOnboardingState() == OnboardingState.VERIFIED) {
            user.transitionTo(OnboardingState.REJECTED);
        }
        userRepo.save(user);

        // Notify user
        notificationGateway.sendInApp(user.getId(),
                "Approval Not Granted",
                "Your request for " + request.getRequestedRole() + " role was not approved. Reason: " + note);
    }
}
