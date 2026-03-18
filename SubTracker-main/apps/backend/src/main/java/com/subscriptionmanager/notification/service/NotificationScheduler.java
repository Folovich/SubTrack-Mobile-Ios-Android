package com.subscriptionmanager.notification.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {
    private final SmartNotificationService smartNotificationService;

    public NotificationScheduler(SmartNotificationService smartNotificationService) {
        this.smartNotificationService = smartNotificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scheduleSmartNotifications() {
        smartNotificationService.generateSmartNotifications();
    }
}
