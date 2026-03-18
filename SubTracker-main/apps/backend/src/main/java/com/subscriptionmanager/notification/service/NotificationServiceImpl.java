package com.subscriptionmanager.notification.service;

import com.subscriptionmanager.common.enums.NotificationType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Notification;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.notification.dto.NotificationResponse;
import com.subscriptionmanager.repository.NotificationRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;
    private static final int UPCOMING_MIN_DAYS = 1;
    private static final int UPCOMING_MAX_DAYS = 3;
    private static final String STATUS_PENDING = "PENDING";
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.UTC;
    private static final LocalTime DEFAULT_NOTIFICATION_TIME = LocalTime.of(9, 0);

    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SmartNotificationService smartNotificationService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            SubscriptionRepository subscriptionRepository,
            SmartNotificationService smartNotificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.smartNotificationService = smartNotificationService;
    }

    @Override
    @Transactional
    public List<NotificationResponse> list(Long userId, int days) {
        validateDays(days);
        ensureUpcomingChargeNotifications(userId);
        smartNotificationService.generateSmartNotificationsForUser(userId);

        LocalDate today = LocalDate.now();
        OffsetDateTime from = today.atStartOfDay().atOffset(DEFAULT_OFFSET);
        OffsetDateTime to = today.plusDays(days).atTime(LocalTime.MAX).atOffset(DEFAULT_OFFSET);

        return notificationRepository.findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(userId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureUpcomingChargeNotifications(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate maxUpcomingDate = today.plusDays(UPCOMING_MAX_DAYS);

        List<Subscription> dueSoonSubscriptions =
                subscriptionRepository.findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                        userId,
                        SubscriptionStatus.ACTIVE,
                        today.plusDays(UPCOMING_MIN_DAYS),
                        maxUpcomingDate
                );

        for (Subscription subscription : dueSoonSubscriptions) {
            long daysUntilBilling = ChronoUnit.DAYS.between(today, subscription.getNextBillingDate());
            if (daysUntilBilling < UPCOMING_MIN_DAYS || daysUntilBilling > UPCOMING_MAX_DAYS) {
                continue;
            }

            OffsetDateTime scheduledAt = today.atTime(DEFAULT_NOTIFICATION_TIME).atOffset(DEFAULT_OFFSET);
            boolean alreadyExists = notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndScheduledAt(
                    userId,
                    subscription.getId(),
                    NotificationType.UPCOMING_CHARGE,
                    scheduledAt
            );

            if (alreadyExists) {
                continue;
            }

            Notification notification = new Notification();
            notification.setUser(subscription.getUser());
            notification.setSubscription(subscription);
            notification.setType(NotificationType.UPCOMING_CHARGE);
            notification.setMessage(buildUpcomingChargeMessage(subscription, daysUntilBilling));
            notification.setScheduledAt(scheduledAt);
            notification.setStatus(STATUS_PENDING);
            notificationRepository.save(notification);
        }
    }

    private String buildUpcomingChargeMessage(Subscription subscription, long daysUntilBilling) {
        String dayWord = daysUntilBilling == 1 ? "day" : "days";
        return String.format(
                Locale.ROOT,
                "%s will charge %s %s in %d %s (billing date: %s)",
                subscription.getServiceName(),
                subscription.getAmount(),
                subscription.getCurrency(),
                daysUntilBilling,
                dayWord,
                subscription.getNextBillingDate()
        );
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getMessage(),
                notification.getScheduledAt().toString(),
                notification.getStatus()
        );
    }

    private void validateDays(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new ApiException("days must be between 1 and 365");
        }
    }
}
