package com.sanjeevanigrid.identity.api;

import com.sanjeevanigrid.identity.domain.ApprovalRequest;
import com.sanjeevanigrid.identity.domain.ApprovalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/approvals")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequest>> getPending() {
        return ResponseEntity.ok(approvalService.getPendingRequests());
    }

    public record ReviewRequest(String note) {}

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewRequest request,
            Authentication auth) {
        UUID reviewerId = UUID.fromString(auth.getName());
        approvalService.approve(id, reviewerId, request != null ? request.note() : null);
        return ResponseEntity.ok(Map.of("message", "Approved"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        UUID reviewerId = UUID.fromString(auth.getName());
        approvalService.reject(id, reviewerId, request.note());
        return ResponseEntity.ok(Map.of("message", "Rejected"));
    }
}
