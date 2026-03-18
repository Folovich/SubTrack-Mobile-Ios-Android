package com.subscriptionmanager.notification.dto;

public record NotificationResponse(Long id, String type, String message, String scheduledAt, String status) {
}
