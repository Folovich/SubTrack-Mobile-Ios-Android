package com.subscriptionmanager.notification.service;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.NotificationType;
import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Notification;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.NotificationRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SmartNotificationService smartNotificationService;

    @Test
    void listCreatesUpcomingReminderForDueSoonSubscription() {
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                subscriptionRepository,
                smartNotificationService
        );

        LocalDate today = LocalDate.now();
        Subscription subscription = new Subscription();
        subscription.setId(15L);
        subscription.setServiceName("Spotify");
        subscription.setAmount(BigDecimal.valueOf(7.99));
        subscription.setCurrency("USD");
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSourceType(SourceType.MANUAL);
        subscription.setNextBillingDate(today.plusDays(2));

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        subscription.setUser(user);

        when(subscriptionRepository.findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                1L,
                SubscriptionStatus.ACTIVE,
                today.plusDays(1),
                today.plusDays(3)
        )).thenReturn(List.of(subscription));

        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndScheduledAt(
                eq(1L),
                eq(15L),
                eq(NotificationType.UPCOMING_CHARGE),
                any(OffsetDateTime.class)
        )).thenReturn(false);

        when(notificationRepository.findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                eq(1L),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        service.list(1L, 7);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("PENDING", saved.getStatus());
        assertEquals(NotificationType.UPCOMING_CHARGE, saved.getType());
        assertTrue(saved.getMessage().contains("Spotify"));
    }

    @Test
    void listThrowsForInvalidDays() {
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                subscriptionRepository,
                smartNotificationService
        );
        assertThrows(ApiException.class, () -> service.list(1L, 0));
    }

    @Test
    void repeatedListCallDoesNotCreateDuplicateReminder() {
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                subscriptionRepository,
                smartNotificationService
        );

        LocalDate today = LocalDate.now();
        Subscription subscription = new Subscription();
        subscription.setId(21L);
        subscription.setServiceName("YouTube Premium");
        subscription.setAmount(BigDecimal.valueOf(12.99));
        subscription.setCurrency("USD");
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSourceType(SourceType.MANUAL);
        subscription.setNextBillingDate(today.plusDays(2));

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        subscription.setUser(user);

        when(subscriptionRepository.findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                1L,
                SubscriptionStatus.ACTIVE,
                today.plusDays(1),
                today.plusDays(3)
        )).thenReturn(List.of(subscription));

        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndScheduledAt(
                eq(1L),
                eq(21L),
                eq(NotificationType.UPCOMING_CHARGE),
                any(OffsetDateTime.class)
        )).thenReturn(false, true);

        when(notificationRepository.findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                eq(1L),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        service.list(1L, 7);
        service.list(1L, 7);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void listTriggersSmartNotificationGenerationForCurrentUser() {
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                subscriptionRepository,
                smartNotificationService
        );

        LocalDate today = LocalDate.now();
        when(subscriptionRepository.findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                1L,
                SubscriptionStatus.ACTIVE,
                today.plusDays(1),
                today.plusDays(3)
        )).thenReturn(List.of());

        when(notificationRepository.findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                eq(1L),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        service.list(1L, 7);

        verify(smartNotificationService).generateSmartNotificationsForUser(1L);
    }
}
