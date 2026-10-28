package com.sanjeevanigrid.integration.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanjeevanigrid.common.event.AppointmentShifted;
import com.sanjeevanigrid.common.event.BloodRequestCreated;
import com.sanjeevanigrid.common.event.StockLow;
import com.sanjeevanigrid.common.event.SurgePredicted;
import com.sanjeevanigrid.integration.domain.WebhookOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for domain events across modules and enqueues them in the Webhook Outbox.
 */
@Component
public class WebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventListener.class);

    private final WebhookOutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public WebhookEventListener(WebhookOutboxRepository outboxRepo, ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onStockLow(StockLow event) {
        enqueueWebhook(event.hospitalId(), "STOCK_LOW", event);
    }

    @EventListener
    public void onSurgePredicted(SurgePredicted event) {
        enqueueWebhook(null, "SURGE_PREDICTED", event);
    }

    @EventListener
    public void onAppointmentShifted(AppointmentShifted event) {
        enqueueWebhook(null, "APPOINTMENT_SHIFTED", event);
    }

    @EventListener
    public void onBloodRequestCreated(BloodRequestCreated event) {
        enqueueWebhook(event.hospitalId(), "BLOOD_REQUEST_CREATED", event);
    }

    private void enqueueWebhook(java.util.UUID hospitalId, String eventType, Object payloadObject) {
        try {
            String json = objectMapper.writeValueAsString(payloadObject);
            String targetUrl = "https://hospital-ehr-webhook.demo/api/events";
            WebhookOutbox outbox = new WebhookOutbox(hospitalId, eventType, json, targetUrl);
            outboxRepo.save(outbox);
            log.info("[WEBHOOK_OUTBOX] Enqueued event '{}' for hospital {}: {}", eventType, hospitalId, outbox.getId());
        } catch (Exception e) {
            log.warn("Failed to serialize webhook payload: {}", e.getMessage());
        }
    }
}
