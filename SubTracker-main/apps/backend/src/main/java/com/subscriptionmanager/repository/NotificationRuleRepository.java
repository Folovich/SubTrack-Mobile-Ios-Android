package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long> {
    List<NotificationRule> findByUserId(Long userId);
}
