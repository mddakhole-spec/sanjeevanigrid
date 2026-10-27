package com.sanjeevanigrid.notification.api;

import com.sanjeevanigrid.notification.domain.Notification;
import com.sanjeevanigrid.notification.infra.NotificationRepository;
import com.sanjeevanigrid.notification.infra.SmsSandboxAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationRepository notificationRepo;
    private final SmsSandboxAdapter smsSandbox;

    public NotificationController(NotificationRepository notificationRepo,
                                  SmsSandboxAdapter smsSandbox) {
        this.notificationRepo = notificationRepo;
        this.smsSandbox = smsSandbox;
    }

    /**
     * Get current user's in-app notifications.
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(notificationRepo.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * Get unread count for current user.
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(Map.of("count", notificationRepo.countByUserIdAndReadFalse(userId)));
    }

    /**
     * Mark a notification as read.
     */
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        notificationRepo.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.markRead();
                notificationRepo.save(n);
            }
        });
        return ResponseEntity.ok().build();
    }

    // ==================== SMS Sandbox Inbox (public for demo) ====================

    /**
     * View the SMS sandbox inbox (all messages).
     */
    @GetMapping("/sms-inbox")
    public ResponseEntity<List<SmsSandboxAdapter.SmsMessage>> getSmsInbox() {
        return ResponseEntity.ok(smsSandbox.getInbox());
    }

    /**
     * View SMS sandbox inbox for a specific phone number.
     */
    @GetMapping("/sms-inbox/{phone}")
    public ResponseEntity<List<SmsSandboxAdapter.SmsMessage>> getSmsInboxForPhone(@PathVariable String phone) {
        return ResponseEntity.ok(smsSandbox.getInboxForPhone(phone));
    }
}
