package com.subscriptionmanager.notification.controller;

import com.subscriptionmanager.notification.dto.NotificationResponse;
import com.subscriptionmanager.notification.service.NotificationService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam(defaultValue = "7") int days) {
        Long userId = currentUserService.requireCurrentUserId();
        return notificationService.list(userId, days);
    }
}
