package com.subscriptionmanager.notification.service;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.NotificationType;
import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Notification;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.SubscriptionEvent;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.entity.UsageSignal;
import com.subscriptionmanager.repository.NotificationRepository;
import com.subscriptionmanager.repository.SubscriptionEventRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartNotificationServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionEventRepository subscriptionEventRepository;

    @Mock
    private UsageSignalRepository usageSignalRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void generateForUserCreatesPriceChangeNotification() {
        SmartNotificationServiceImpl service = new SmartNotificationServiceImpl(
                subscriptionRepository,
                subscriptionEventRepository,
                usageSignalRepository,
                notificationRepository,
                14
        );

        Subscription subscription = subscription(1L, 100L, "Netflix", BigDecimal.valueOf(12.99), OffsetDateTime.now().minusDays(10));
        SubscriptionEvent event = new SubscriptionEvent();
        event.setSubscription(subscription);
        event.setEventType("PRICE_CHANGE");
        event.setOldAmount(BigDecimal.valueOf(9.99));
        event.setNewAmount(BigDecimal.valueOf(12.99));
        event.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(subscriptionEventRepository.findByEventTypeAndSubscriptionUserIdOrderByCreatedAtAsc("PRICE_CHANGE", 1L))
                .thenReturn(List.of(event));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndMessage(
                eq(1L),
                eq(100L),
                eq(NotificationType.PRICE_CHANGE),
                any(String.class)
        )).thenReturn(false);

        service.generateSmartNotificationsForUser(1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(NotificationType.PRICE_CHANGE, saved.getType());
        assertTrue(saved.getMessage().contains("Netflix"));
        assertTrue(saved.getMessage().contains("9.99"));
        assertTrue(saved.getMessage().contains("12.99"));
    }

    @Test
    void generateForUserDoesNotDuplicatePriceChangeNotifications() {
        SmartNotificationServiceImpl service = new SmartNotificationServiceImpl(
                subscriptionRepository,
                subscriptionEventRepository,
                usageSignalRepository,
                notificationRepository,
                14
        );

        Subscription subscription = subscription(1L, 101L, "Spotify", BigDecimal.valueOf(11.99), OffsetDateTime.now().minusDays(20));
        SubscriptionEvent event = new SubscriptionEvent();
        event.setSubscription(subscription);
        event.setEventType("PRICE_CHANGE");
        event.setOldAmount(BigDecimal.valueOf(9.99));
        event.setNewAmount(BigDecimal.valueOf(11.99));
        event.setCreatedAt(OffsetDateTime.now().minusDays(2));

        when(subscriptionEventRepository.findByEventTypeAndSubscriptionUserIdOrderByCreatedAtAsc("PRICE_CHANGE", 1L))
                .thenReturn(List.of(event));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndMessage(
                eq(1L),
                eq(101L),
                eq(NotificationType.PRICE_CHANGE),
                any(String.class)
        )).thenReturn(false, true);

        service.generateSmartNotificationsForUser(1L);
        service.generateSmartNotificationsForUser(1L);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void generateForUserCreatesInactivityNotificationWhenSignalsAreMissing() {
        SmartNotificationServiceImpl service = new SmartNotificationServiceImpl(
                subscriptionRepository,
                subscriptionEventRepository,
                usageSignalRepository,
                notificationRepository,
                14
        );

        OffsetDateTime staleUpdatedAt = OffsetDateTime.now().minusDays(20);
        Subscription subscription = subscription(1L, 102L, "YouTube Premium", BigDecimal.valueOf(8.99), staleUpdatedAt);

        when(subscriptionEventRepository.findByEventTypeAndSubscriptionUserIdOrderByCreatedAtAsc("PRICE_CHANGE", 1L))
                .thenReturn(List.of());
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(usageSignalRepository.findTopByUserIdAndSubscriptionIdOrderByCreatedAtDesc(1L, 102L))
                .thenReturn(Optional.empty());
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndScheduledAtGreaterThanEqual(
                1L,
                102L,
                NotificationType.INACTIVITY,
                staleUpdatedAt
        )).thenReturn(false);

        service.generateSmartNotificationsForUser(1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(NotificationType.INACTIVITY, saved.getType());
        assertTrue(saved.getMessage().contains("YouTube Premium"));
        assertTrue(saved.getMessage().contains("14+"));
    }

    @Test
    void generateForUserDoesNotDuplicateInactivityNotifications() {
        SmartNotificationServiceImpl service = new SmartNotificationServiceImpl(
                subscriptionRepository,
                subscriptionEventRepository,
                usageSignalRepository,
                notificationRepository,
                14
        );

        OffsetDateTime lastSignalAt = OffsetDateTime.now().minusDays(30);
        Subscription subscription = subscription(1L, 103L, "HBO Max", BigDecimal.valueOf(5.99), OffsetDateTime.now().minusDays(40));

        UsageSignal signal = new UsageSignal();
        signal.setCreatedAt(lastSignalAt);

        when(subscriptionEventRepository.findByEventTypeAndSubscriptionUserIdOrderByCreatedAtAsc("PRICE_CHANGE", 1L))
                .thenReturn(List.of());
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(subscription));
        when(usageSignalRepository.findTopByUserIdAndSubscriptionIdOrderByCreatedAtDesc(1L, 103L))
                .thenReturn(Optional.of(signal));
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndScheduledAtGreaterThanEqual(
                1L,
                103L,
                NotificationType.INACTIVITY,
                lastSignalAt
        )).thenReturn(false, true);

        service.generateSmartNotificationsForUser(1L);
        service.generateSmartNotificationsForUser(1L);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    private Subscription subscription(
            Long userId,
            Long subscriptionId,
            String serviceName,
            BigDecimal amount,
            OffsetDateTime updatedAt
    ) {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUser(user);
        subscription.setServiceName(serviceName);
        subscription.setAmount(amount);
        subscription.setCurrency("USD");
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSourceType(SourceType.MANUAL);
        subscription.setNextBillingDate(LocalDate.now().plusDays(5));
        subscription.setCreatedAt(updatedAt.minusDays(10));
        subscription.setUpdatedAt(updatedAt);
        return subscription;
    }
}
