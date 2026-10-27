package com.sanjeevanigrid.notification.infra;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SMS Sandbox adapter — logs SMS messages to console and stores in memory.
 * Used in sandbox mode for demo purposes. In-memory "inbox" viewable via API.
 */
@Component
public class SmsSandboxAdapter implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(SmsSandboxAdapter.class);

    public record SmsMessage(String phone, String message, Instant timestamp) {}

    private final List<SmsMessage> inbox = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendInApp(UUID userId, String title, String body) {
        // SmsSandbox does not handle in-app notifications
    }

    @Override
    public void sendSms(String phone, String message) {
        SmsMessage sms = new SmsMessage(phone, message, Instant.now());
        inbox.add(sms);
        log.info("[SMS_SANDBOX] To: {} | Message: {}", phone, message);
    }

    @Override
    public String getChannel() {
        return "SMS_SANDBOX";
    }

    /**
     * Get all SMS messages in the sandbox inbox (for demo/testing).
     */
    public List<SmsMessage> getInbox() {
        return Collections.unmodifiableList(inbox);
    }

    /**
     * Get SMS messages for a specific phone number.
     */
    public List<SmsMessage> getInboxForPhone(String phone) {
        return inbox.stream().filter(m -> m.phone().equals(phone)).toList();
    }

    /**
     * Clear the sandbox inbox.
     */
    public void clearInbox() {
        inbox.clear();
    }
}
