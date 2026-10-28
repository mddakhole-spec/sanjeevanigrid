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
    private final com.sanjeevanigrid.notification.infra.WhatsAppLinkAdapter whatsAppAdapter;
    private final com.sanjeevanigrid.notification.infra.TelegramAdapter telegramAdapter;

    public NotificationController(NotificationRepository notificationRepo,
                                  SmsSandboxAdapter smsSandbox,
                                  com.sanjeevanigrid.notification.infra.WhatsAppLinkAdapter whatsAppAdapter,
                                  com.sanjeevanigrid.notification.infra.TelegramAdapter telegramAdapter) {
        this.notificationRepo = notificationRepo;
        this.smsSandbox = smsSandbox;
        this.whatsAppAdapter = whatsAppAdapter;
        this.telegramAdapter = telegramAdapter;
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

    /**
     * Clear all messages in SMS sandbox inbox.
     */
    @DeleteMapping("/sms-inbox")
    public ResponseEntity<Map<String, String>> clearSmsInbox() {
        smsSandbox.clearInbox();
        return ResponseEntity.ok(Map.of("message", "SMS Inbox cleared"));
    }

    /**
     * Simulate an incoming SMS reply from a donor or patient.
     */
    @PostMapping("/sms-inbox/simulate-reply")
    public ResponseEntity<Map<String, Object>> simulateReply(@RequestBody Map<String, String> body) {
        String phone = body.getOrDefault("phone", "+919822100001");
        String message = body.getOrDefault("message", "YES, I am available to donate blood immediately.");
        smsSandbox.sendSms(phone, "[INCOMING REPLY] " + message);
        return ResponseEntity.ok(Map.of("status", "RECEIVED", "phone", phone, "reply", message));
    }

    // ==================== WhatsApp & Telegram Outbox (Demo Inspection) ====================

    @GetMapping("/whatsapp-outbox")
    public ResponseEntity<List<com.sanjeevanigrid.notification.infra.WhatsAppLinkAdapter.WhatsAppDispatch>> getWhatsAppOutbox() {
        return ResponseEntity.ok(whatsAppAdapter.getOutbox());
    }

    @GetMapping("/telegram-status")
    public ResponseEntity<Map<String, Object>> getTelegramStatus() {
        return ResponseEntity.ok(Map.of(
                "enabled", telegramAdapter.isEnabled(),
                "channel", telegramAdapter.getChannel(),
                "dispatches", telegramAdapter.getDispatches()
        ));
    }

    @PostMapping("/telegram-toggle")
    public ResponseEntity<Map<String, Object>> toggleTelegram(@RequestParam boolean enabled) {
        telegramAdapter.setEnabled(enabled);
        return ResponseEntity.ok(Map.of(
                "enabled", telegramAdapter.isEnabled(),
                "message", "Telegram adapter enabled status updated"
        ));
    }
}
