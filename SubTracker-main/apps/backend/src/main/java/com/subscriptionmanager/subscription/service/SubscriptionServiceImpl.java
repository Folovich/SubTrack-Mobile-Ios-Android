package com.subscriptionmanager.subscription.service;

import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.common.enums.NotificationType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Category;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.SubscriptionEvent;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.CategoryRepository;
import com.subscriptionmanager.repository.SubscriptionEventRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UserRepository;
import com.subscriptionmanager.subscription.dto.SubscriptionRequest;
import com.subscriptionmanager.subscription.dto.SubscriptionResponse;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.List;
import java.math.BigDecimal;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;
    private static final String PRICE_CHANGE_EVENT_TYPE = NotificationType.PRICE_CHANGE.name();

    private final SubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SubscriptionEventRepository subscriptionEventRepository;

    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            SubscriptionEventRepository subscriptionEventRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.subscriptionEventRepository = subscriptionEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list(Long userId) {
        return subscriptionRepository.findByUserIdOrderByNextBillingDateAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingSubscriptionResponse> upcoming(Long userId, int days) {
        validateDays(days);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        return subscriptionRepository.findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                        userId,
                        SubscriptionStatus.ACTIVE,
                        today,
                        endDate
                )
                .stream()
                .map(subscription -> toUpcomingResponse(subscription, today))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getById(Long userId, Long id) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("Subscription not found"));
        return toResponse(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResponse create(Long userId, SubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setSourceType(SourceType.MANUAL);
        applyRequest(subscription, request);

        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SubscriptionResponse update(Long userId, Long id, SubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("Subscription not found"));

        BigDecimal oldAmount = subscription.getAmount();
        applyRequest(subscription, request);
        Subscription saved = subscriptionRepository.save(subscription);
        savePriceChangeEventIfNeeded(saved, oldAmount);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("Subscription not found"));
        subscriptionRepository.delete(subscription);
    }

    private void applyRequest(Subscription subscription, SubscriptionRequest request) {
        subscription.setServiceName(request.serviceName().trim());
        subscription.setAmount(request.amount());
        subscription.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        subscription.setBillingPeriod(request.billingPeriod());
        subscription.setNextBillingDate(request.nextBillingDate());
        subscription.setStatus(request.status() == null ? SubscriptionStatus.ACTIVE : request.status());

        if (request.categoryId() == null) {
            subscription.setCategory(null);
            return;
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ApiException("Category not found"));
        subscription.setCategory(category);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        String categoryName = subscription.getCategory() == null ? null : subscription.getCategory().getName();
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getServiceName(),
                categoryName,
                subscription.getAmount(),
                subscription.getCurrency(),
                subscription.getBillingPeriod(),
                subscription.getNextBillingDate(),
                subscription.getStatus()
        );
    }

    private UpcomingSubscriptionResponse toUpcomingResponse(Subscription subscription, LocalDate today) {
        String categoryName = subscription.getCategory() == null ? null : subscription.getCategory().getName();
        long daysUntilBilling = ChronoUnit.DAYS.between(today, subscription.getNextBillingDate());

        return new UpcomingSubscriptionResponse(
                subscription.getId(),
                subscription.getServiceName(),
                categoryName,
                subscription.getAmount(),
                subscription.getCurrency(),
                subscription.getBillingPeriod(),
                subscription.getNextBillingDate(),
                subscription.getStatus(),
                daysUntilBilling
        );
    }

    private void validateDays(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new ApiException("days must be between 1 and 365");
        }
    }

    private void savePriceChangeEventIfNeeded(Subscription subscription, BigDecimal oldAmount) {
        BigDecimal newAmount = subscription.getAmount();
        if (oldAmount == null || newAmount == null || oldAmount.compareTo(newAmount) == 0) {
            return;
        }

        SubscriptionEvent event = new SubscriptionEvent();
        event.setSubscription(subscription);
        event.setEventType(PRICE_CHANGE_EVENT_TYPE);
        event.setOldAmount(oldAmount);
        event.setNewAmount(newAmount);
        subscriptionEventRepository.save(event);
    }
}
