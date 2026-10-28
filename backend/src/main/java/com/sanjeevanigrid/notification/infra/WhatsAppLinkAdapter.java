package com.sanjeevanigrid.notification.infra;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * WhatsApp Link Adapter — Generates https://wa.me/{phone}?text={encoded} prefilled links.
 * In sandbox mode, logs the link, adds to an audit outbox, and returns the clickable URL.
 */
@Component
public class WhatsAppLinkAdapter implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppLinkAdapter.class);

    public record WhatsAppDispatch(
            String phone,
            String cleanPhone,
            String message,
            String waLink,
            Instant timestamp
    ) {}

    private final List<WhatsAppDispatch> outbox = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendInApp(UUID userId, String title, String body) {
        // WhatsAppLinkAdapter handles WhatsApp links
    }

    @Override
    public void sendSms(String phone, String message) {
        sendWhatsApp(phone, message);
    }

    @Override
    public String sendWhatsApp(String phone, String message) {
        String cleanPhone = sanitizePhoneNumber(phone);
        String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
        String waLink = "https://wa.me/" + cleanPhone + "?text=" + encodedMsg;

        WhatsAppDispatch dispatch = new WhatsAppDispatch(phone, cleanPhone, message, waLink, Instant.now());
        outbox.add(dispatch);

        log.info("[WHATSAPP_LINK] Generated for {} -> {}", cleanPhone, waLink);
        return waLink;
    }

    @Override
    public String getChannel() {
        return "WHATSAPP_LINK";
    }

    public List<WhatsAppDispatch> getOutbox() {
        return Collections.unmodifiableList(outbox);
    }

    public void clearOutbox() {
        outbox.clear();
    }

    /**
     * Normalizes phone number into international format without '+' or special characters.
     * Default assumption: Indian numbers without country code get '91' prefixed.
     */
    public static String sanitizePhoneNumber(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;
        }
        return digits;
    }
}
