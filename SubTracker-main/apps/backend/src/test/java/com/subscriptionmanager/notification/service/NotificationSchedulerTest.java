package com.subscriptionmanager.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private SmartNotificationService smartNotificationService;

    @Test
    void schedulerTriggersSmartNotificationJob() {
        NotificationScheduler scheduler = new NotificationScheduler(smartNotificationService);

        scheduler.scheduleSmartNotifications();

        verify(smartNotificationService).generateSmartNotifications();
    }
}
