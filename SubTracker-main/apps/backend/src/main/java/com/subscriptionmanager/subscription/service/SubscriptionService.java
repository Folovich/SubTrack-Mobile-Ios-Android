package com.subscriptionmanager.subscription.service;

import com.subscriptionmanager.subscription.dto.SubscriptionRequest;
import com.subscriptionmanager.subscription.dto.SubscriptionResponse;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;

import java.util.List;

public interface SubscriptionService {
    List<SubscriptionResponse> list(Long userId);
    List<UpcomingSubscriptionResponse> upcoming(Long userId, int days);
    SubscriptionResponse getById(Long userId, Long id);
    SubscriptionResponse create(Long userId, SubscriptionRequest request);
    SubscriptionResponse update(Long userId, Long id, SubscriptionRequest request);
    void delete(Long userId, Long id);
}
