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

    /**
     * Returns the notification channel name (e.g., "IN_APP", "SMS_SANDBOX").
     */
    String getChannel();
}
