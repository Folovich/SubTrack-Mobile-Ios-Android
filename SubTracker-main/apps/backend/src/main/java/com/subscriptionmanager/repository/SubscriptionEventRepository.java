package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {
    List<SubscriptionEvent> findByEventTypeOrderByCreatedAtAsc(String eventType);

    List<SubscriptionEvent> findByEventTypeAndSubscriptionUserIdOrderByCreatedAtAsc(String eventType, Long userId);
}
