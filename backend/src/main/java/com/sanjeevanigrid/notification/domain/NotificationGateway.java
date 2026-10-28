package com.sanjeevanigrid.notification.domain;

import java.util.UUID;

/**
 * Notification gateway interface (adapter pattern).
 * Implementations: InApp, SmsSandbox, WhatsAppLink, Telegram.
 * Config flag mode=sandbox|live determines active adapters.
 */
public interface NotificationGateway {

    void sendInApp(UUID userId, String title, String body);

    void sendSms(String phone, String message);

    default String sendWhatsApp(String phone, String message) {
        return "";
    }

    default boolean sendTelegram(String chatId, String message) {
        return false;
    }

    /**
     * Returns the notification channel name (e.g., "IN_APP", "SMS_SANDBOX", "WHATSAPP_LINK", "TELEGRAM").
     */
    String getChannel();
}
