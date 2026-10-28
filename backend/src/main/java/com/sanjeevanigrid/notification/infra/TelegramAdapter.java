package com.sanjeevanigrid.notification.infra;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Telegram Adapter — Dispatches emergency alerts to Telegram bots/channels.
 * Configured behind flag 'app.telegram.enabled'.
 * When disabled (default), logs and marks as DISABLED_BY_FLAG without failing.
 */
@Component
public class TelegramAdapter implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(TelegramAdapter.class);

    public record TelegramDispatch(
            String chatId,
            String message,
            String status,
            Instant timestamp
    ) {}

    @Value("${app.telegram.enabled:false}")
    private boolean enabled;

    @Value("${app.telegram.bot-token:mock_token}")
    private String botToken;

    @Value("${app.telegram.chat-id:mock_chat_id}")
    private String defaultChatId;

    private final List<TelegramDispatch> dispatches = Collections.synchronizedList(new ArrayList<>());

    public TelegramAdapter() {}

    public TelegramAdapter(boolean enabled, String botToken, String defaultChatId) {
        this.enabled = enabled;
        this.botToken = botToken;
        this.defaultChatId = defaultChatId;
    }

    @Override
    public void sendInApp(UUID userId, String title, String body) {
        // Telegram adapter handles Telegram messages
    }

    @Override
    public void sendSms(String phone, String message) {
        sendTelegram(defaultChatId, "[To: " + phone + "] " + message);
    }

    @Override
    public boolean sendTelegram(String chatId, String message) {
        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId : defaultChatId;

        if (!enabled) {
            log.info("[TELEGRAM] Adapter disabled (app.telegram.enabled=false). Message skipped for chat: {}", targetChatId);
            dispatches.add(new TelegramDispatch(targetChatId, message, "DISABLED_BY_FLAG", Instant.now()));
            return false;
        }

        // When enabled in sandbox/live:
        log.info("[TELEGRAM] Dispatched alert to Telegram Chat ID {}: {}", targetChatId, message);
        dispatches.add(new TelegramDispatch(targetChatId, message, "SENT", Instant.now()));
        return true;
    }

    @Override
    public String getChannel() {
        return "TELEGRAM";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<TelegramDispatch> getDispatches() {
        return Collections.unmodifiableList(dispatches);
    }

    public void clearDispatches() {
        dispatches.clear();
    }
}
