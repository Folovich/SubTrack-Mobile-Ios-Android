package com.subscriptionmanager.usage.service;

import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.UsageSignal;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import com.subscriptionmanager.usage.dto.UsageSignalCreateRequest;
import com.subscriptionmanager.usage.dto.UsageSignalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsageSignalServiceImpl implements UsageSignalService {

    private final UsageSignalRepository usageSignalRepository;
    private final SubscriptionRepository subscriptionRepository;

    public UsageSignalServiceImpl(UsageSignalRepository usageSignalRepository, SubscriptionRepository subscriptionRepository) {
        this.usageSignalRepository = usageSignalRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional
    public UsageSignalResponse create(Long userId, UsageSignalCreateRequest request) {
        Subscription subscription = findOwnedSubscription(userId, request.subscriptionId());

        UsageSignal usageSignal = new UsageSignal();
        usageSignal.setUser(subscription.getUser());
        usageSignal.setSubscription(subscription);
        usageSignal.setSignalType(request.signalType().trim());
        usageSignal.setValue(request.value().trim());

        UsageSignal saved = usageSignalRepository.save(usageSignal);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsageSignalResponse> list(Long userId, Long subscriptionId) {
        if (subscriptionId == null) {
            return usageSignalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        Subscription subscription = findOwnedSubscription(userId, subscriptionId);
        return usageSignalRepository.findByUserIdAndSubscriptionIdOrderByCreatedAtDesc(userId, subscription.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Subscription findOwnedSubscription(Long userId, Long subscriptionId) {
        if (subscriptionId == null || subscriptionId <= 0) {
            throw new ApiException("subscriptionId must be greater than 0");
        }
        return subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ApiException("Subscription not found"));
    }

    private UsageSignalResponse toResponse(UsageSignal usageSignal) {
        return new UsageSignalResponse(
                usageSignal.getId(),
                usageSignal.getSubscription().getId(),
                usageSignal.getSignalType(),
                usageSignal.getValue(),
                usageSignal.getCreatedAt().toString()
        );
    }
}
