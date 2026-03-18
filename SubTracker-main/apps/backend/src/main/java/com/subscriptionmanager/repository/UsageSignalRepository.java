package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.UsageSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UsageSignalRepository extends JpaRepository<UsageSignal, Long> {
    List<UsageSignal> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<UsageSignal> findByUserIdAndSubscriptionIdOrderByCreatedAtDesc(Long userId, Long subscriptionId);
    List<UsageSignal> findByUserIdAndCreatedAtBetween(Long userId, OffsetDateTime from, OffsetDateTime to);
    List<UsageSignal> findByUserIdAndSubscriptionIdAndCreatedAtBetween(
            Long userId,
            Long subscriptionId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Optional<UsageSignal> findTopByUserIdAndSubscriptionIdOrderByCreatedAtDesc(Long userId, Long subscriptionId);
}
