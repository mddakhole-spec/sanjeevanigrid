package com.sanjeevanigrid.notification.infra;

import com.sanjeevanigrid.notification.domain.Notification;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * InApp notification adapter — stores notifications in the database.
 * Primary adapter, always active.
 */
@Component
@Primary
public class InAppNotificationAdapter implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationAdapter.class);
    private final NotificationRepository notificationRepo;

    public InAppNotificationAdapter(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @Override
    public void sendInApp(UUID userId, String title, String body) {
        Notification notification = Notification.inApp(userId, title, body);
        notificationRepo.save(notification);
        log.info("[IN_APP] Notification sent to user {}: {}", userId, title);
    }

    @Override
    public void sendSms(String phone, String message) {
        // InApp adapter does not handle SMS; delegate to SmsSandbox
        log.debug("[IN_APP] SMS not supported in InApp adapter, skipping for phone: {}", phone);
    }

    @Override
    public String getChannel() {
        return "IN_APP";
    }
}
