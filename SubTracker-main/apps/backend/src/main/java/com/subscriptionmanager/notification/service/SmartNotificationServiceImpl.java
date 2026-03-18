package com.subscriptionmanager.notification.service;

import com.subscriptionmanager.common.enums.NotificationType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Notification;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.SubscriptionEvent;
import com.subscriptionmanager.entity.UsageSignal;
import com.subscriptionmanager.repository.NotificationRepository;
import com.subscriptionmanager.repository.SubscriptionEventRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
public class SmartNotificationServiceImpl implements SmartNotificationService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String PRICE_CHANGE_EVENT_TYPE = NotificationType.PRICE_CHANGE.name();
    private static final int DEFAULT_INACTIVITY_DAYS = 14;
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.UTC;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventRepository subscriptionEventRepository;
    private final UsageSignalRepository usageSignalRepository;
    private final NotificationRepository notificationRepository;
    private final int inactivityDays;

    public SmartNotificationServiceImpl(
            SubscriptionRepository subscriptionRepository,
            SubscriptionEventRepository subscriptionEventRepository,
            UsageSignalRepository usageSignalRepository,
            NotificationRepository notificationRepository,
            @Value("${app.notifications.inactivity-days:14}") int inactivityDays
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionEventRepository = subscriptionEventRepository;
        this.usageSignalRepository = usageSignalRepository;
        this.notificationRepository = notificationRepository;
        this.inactivityDays = inactivityDays > 0 ? inactivityDays : DEFAULT_INACTIVITY_DAYS;
    }

    @Override
    @Transactional
    public void generateSmartNotifications() {
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_OFFSET);
        generatePriceChangeNotifications(
                subscriptionEventRepository.findByEventTypeOrderByCreatedAtAsc(PRICE_CHANGE_EVENT_TYPE),
                now
        );
        generateInactivityNotifications(
                subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE),
                now
        );
    }

    @Override
    @Transactional
    public void generateSmartNotificationsForUser(Long userId) {
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_OFFSET);
        generatePriceChangeNotifications(
                subscriptionEventRepository.findByEventTypeAndSubscriptionUserIdOrderByCreatedAtAsc(
                        PRICE_CHANGE_EVENT_TYPE,
                        userId
                ),
                now
        );
        generateInactivityNotifications(
                subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE),
                now
        );
    }

    private void generatePriceChangeNotifications(List<SubscriptionEvent> events, OffsetDateTime scheduledAt) {
        for (SubscriptionEvent event : events) {
            Subscription subscription = event.getSubscription();
            if (subscription == null || subscription.getUser() == null) {
                continue;
            }
            if (subscription.getId() == null || subscription.getUser().getId() == null) {
                continue;
            }
            if (event.getOldAmount() == null || event.getNewAmount() == null) {
                continue;
            }

            Long userId = subscription.getUser().getId();
            Long subscriptionId = subscription.getId();
            OffsetDateTime eventCreatedAt = event.getCreatedAt() == null ? scheduledAt : event.getCreatedAt();
            String message = buildPriceChangeMessage(subscription, event, eventCreatedAt);

            boolean alreadyExists = notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndMessage(
                    userId,
                    subscriptionId,
                    NotificationType.PRICE_CHANGE,
                    message
            );
            if (alreadyExists) {
                continue;
            }

            notificationRepository.save(buildNotification(
                    subscription,
                    NotificationType.PRICE_CHANGE,
                    message,
                    scheduledAt
            ));
        }
    }

    private void generateInactivityNotifications(List<Subscription> subscriptions, OffsetDateTime scheduledAt) {
        OffsetDateTime inactivityThreshold = scheduledAt.minusDays(inactivityDays);

        for (Subscription subscription : subscriptions) {
            if (subscription.getUser() == null || subscription.getUser().getId() == null || subscription.getId() == null) {
                continue;
            }

            Long userId = subscription.getUser().getId();
            Long subscriptionId = subscription.getId();
            OffsetDateTime lastActivityAt = resolveLastActivityAt(subscription, scheduledAt);

            if (!lastActivityAt.isBefore(inactivityThreshold)) {
                continue;
            }

            boolean alreadyExists = notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndScheduledAtGreaterThanEqual(
                    userId,
                    subscriptionId,
                    NotificationType.INACTIVITY,
                    lastActivityAt
            );
            if (alreadyExists) {
                continue;
            }

            notificationRepository.save(buildNotification(
                    subscription,
                    NotificationType.INACTIVITY,
                    buildInactivityMessage(subscription, lastActivityAt),
                    scheduledAt
            ));
        }
    }

    private OffsetDateTime resolveLastActivityAt(Subscription subscription, OffsetDateTime fallback) {
        return usageSignalRepository
                .findTopByUserIdAndSubscriptionIdOrderByCreatedAtDesc(
                        subscription.getUser().getId(),
                        subscription.getId()
                )
                .map(UsageSignal::getCreatedAt)
                .orElseGet(() -> {
                    if (subscription.getUpdatedAt() != null) {
                        return subscription.getUpdatedAt();
                    }
                    if (subscription.getCreatedAt() != null) {
                        return subscription.getCreatedAt();
                    }
                    return fallback;
                });
    }

    private Notification buildNotification(
            Subscription subscription,
            NotificationType type,
            String message,
            OffsetDateTime scheduledAt
    ) {
        Notification notification = new Notification();
        notification.setUser(subscription.getUser());
        notification.setSubscription(subscription);
        notification.setType(type);
        notification.setMessage(message);
        notification.setScheduledAt(scheduledAt);
        notification.setStatus(STATUS_PENDING);
        return notification;
    }

    private String buildPriceChangeMessage(
            Subscription subscription,
            SubscriptionEvent event,
            OffsetDateTime eventCreatedAt
    ) {
        return String.format(
                Locale.ROOT,
                "%s price changed from %s %s to %s %s on %s",
                subscription.getServiceName(),
                event.getOldAmount(),
                subscription.getCurrency(),
                event.getNewAmount(),
                subscription.getCurrency(),
                eventCreatedAt.toLocalDate()
        );
    }

    private String buildInactivityMessage(Subscription subscription, OffsetDateTime lastActivityAt) {
        return String.format(
                Locale.ROOT,
                "%s has no usage signals for %d+ days (last activity: %s)",
                subscription.getServiceName(),
                inactivityDays,
                lastActivityAt.toLocalDate()
        );
    }
}
