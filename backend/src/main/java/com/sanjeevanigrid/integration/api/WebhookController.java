package com.sanjeevanigrid.integration.api;

import com.sanjeevanigrid.integration.domain.WebhookOutbox;
import com.sanjeevanigrid.integration.infra.WebhookOutboxRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integration/webhooks")
public class WebhookController {

    private final WebhookOutboxRepository outboxRepo;

    public WebhookController(WebhookOutboxRepository outboxRepo) {
        this.outboxRepo = outboxRepo;
    }

    @GetMapping
    public ResponseEntity<List<WebhookOutbox>> getWebhooks(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(outboxRepo.findByStatusOrderByCreatedAtDesc(status.toUpperCase()));
        }
        return ResponseEntity.ok(outboxRepo.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchWebhook(@PathVariable UUID id) {
        WebhookOutbox webhook = outboxRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook entry not found: " + id));

        webhook.markDelivered();
        WebhookOutbox updated = outboxRepo.save(webhook);

        return ResponseEntity.ok(Map.of(
                "status", "DELIVERED",
                "webhookId", updated.getId(),
                "eventType", updated.getEventType(),
                "attempts", updated.getAttempts(),
                "message", "Simulated webhook HTTP POST delivery to subscriber endpoint: " + updated.getTargetUrl()
        ));
    }
}
