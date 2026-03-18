package com.subscriptionmanager.repository;

import com.subscriptionmanager.common.enums.NotificationType;
import com.subscriptionmanager.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByScheduledAtDesc(Long userId);

    List<Notification> findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            Long userId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    boolean existsByUserIdAndSubscriptionIdAndTypeAndScheduledAt(
            Long userId,
            Long subscriptionId,
            NotificationType type,
            OffsetDateTime scheduledAt
    );

    boolean existsByUserIdAndSubscriptionIdAndTypeAndMessage(
            Long userId,
            Long subscriptionId,
            NotificationType type,
            String message
    );

    boolean existsByUserIdAndSubscriptionIdAndTypeAndScheduledAtGreaterThanEqual(
            Long userId,
            Long subscriptionId,
            NotificationType type,
            OffsetDateTime scheduledAt
    );
}
