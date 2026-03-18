package com.subscriptionmanager.notification.service;

import com.subscriptionmanager.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> list(Long userId, int days);
}
