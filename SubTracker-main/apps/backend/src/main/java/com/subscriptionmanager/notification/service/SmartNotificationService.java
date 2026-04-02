package com.subscriptionmanager.notification.service;

public interface SmartNotificationService {
    void generateSmartNotifications();

    void generateSmartNotificationsForUser(Long userId);
}
