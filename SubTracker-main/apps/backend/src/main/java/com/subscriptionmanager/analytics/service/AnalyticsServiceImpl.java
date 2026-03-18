package com.subscriptionmanager.analytics.service;

import com.subscriptionmanager.analytics.AnalyticsPeriod;
import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsCategoryItemResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsUsageItemResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsUsageResponse;
import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.UsageSignal;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String MIXED_CURRENCY = "MIXED";
    private static final String UNCATEGORIZED = "Uncategorized";
    private static final ZoneOffset ANALYTICS_ZONE_OFFSET = ZoneOffset.UTC;

    private final SubscriptionRepository subscriptionRepository;
    private final UsageSignalRepository usageSignalRepository;

    public AnalyticsServiceImpl(
            SubscriptionRepository subscriptionRepository,
            UsageSignalRepository usageSignalRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageSignalRepository = usageSignalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(Long userId, AnalyticsPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.rangeStart(today);
        LocalDate to = period.rangeEnd(today);

        List<SubscriptionSpend> spends = calculateSpendsForRange(userId, from, to);

        return new AnalyticsSummaryResponse(
                period.apiValue(),
                from,
                to,
                totalAmount(spends),
                spends.size(),
                resolveCurrency(spends)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsByCategoryResponse byCategory(Long userId, AnalyticsPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.rangeStart(today);
        LocalDate to = period.rangeEnd(today);

        List<SubscriptionSpend> spends = calculateSpendsForRange(userId, from, to);
        BigDecimal total = totalAmount(spends);

        Map<String, BigDecimal> amountByCategory = new LinkedHashMap<>();
        Map<String, Integer> subscriptionsCountByCategory = new LinkedHashMap<>();

        for (SubscriptionSpend spend : spends) {
            String category = resolveCategory(spend.subscription());
            amountByCategory.merge(category, spend.amount(), BigDecimal::add);
            subscriptionsCountByCategory.merge(category, 1, Integer::sum);
        }

        List<AnalyticsCategoryItemResponse> items = amountByCategory.entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new AnalyticsCategoryItemResponse(
                        entry.getKey(),
                        money(entry.getValue()),
                        percent(entry.getValue(), total),
                        subscriptionsCountByCategory.getOrDefault(entry.getKey(), 0)
                ))
                .toList();

        return new AnalyticsByCategoryResponse(
                period.apiValue(),
                from,
                to,
                total,
                resolveCurrency(spends),
                items
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsForecastResponse forecast(Long userId) {
        LocalDate today = LocalDate.now();

        LocalDate monthFrom = AnalyticsPeriod.MONTH.rangeStart(today);
        LocalDate monthTo = AnalyticsPeriod.MONTH.rangeEnd(today);

        LocalDate yearFrom = AnalyticsPeriod.YEAR.rangeStart(today);
        LocalDate yearTo = AnalyticsPeriod.YEAR.rangeEnd(today);

        List<SubscriptionSpend> monthSpends = calculateSpendsForRange(userId, monthFrom, monthTo);
        List<SubscriptionSpend> yearSpends = calculateSpendsForRange(userId, yearFrom, yearTo);

        return new AnalyticsForecastResponse(
                totalAmount(monthSpends),
                totalAmount(yearSpends),
                resolveCurrency(yearSpends)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsUsageResponse usage(Long userId, AnalyticsPeriod period, Long subscriptionId) {
        LocalDate today = LocalDate.now();
        LocalDate from = period.rangeStart(today);
        LocalDate to = period.rangeEnd(today);

        List<Subscription> activeSubscriptions =
                subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        List<Subscription> subscriptionsInScope = activeSubscriptions;

        if (subscriptionId != null) {
            validateSubscriptionId(subscriptionId);
            subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                    .orElseThrow(() -> new ApiException("Subscription not found"));
            subscriptionsInScope = activeSubscriptions.stream()
                    .filter(subscription -> subscriptionId.equals(subscription.getId()))
                    .toList();
        }

        OffsetDateTime fromDateTime = rangeStartDateTime(from);
        OffsetDateTime toDateTime = rangeEndDateTime(to);

        List<UsageSignal> usageSignals = subscriptionId == null
                ? usageSignalRepository.findByUserIdAndCreatedAtBetween(userId, fromDateTime, toDateTime)
                : usageSignalRepository.findByUserIdAndSubscriptionIdAndCreatedAtBetween(
                        userId,
                        subscriptionId,
                        fromDateTime,
                        toDateTime
                );

        Set<Long> activeSubscriptionIdsInScope = subscriptionsInScope.stream()
                .map(Subscription::getId)
                .collect(Collectors.toSet());

        List<UsageSignal> scopedSignals = usageSignals.stream()
                .filter(signal -> activeSubscriptionIdsInScope.contains(signal.getSubscription().getId()))
                .toList();

        Map<Long, Long> signalsCountBySubscription = new HashMap<>();
        Map<Long, OffsetDateTime> lastSignalAtBySubscription = new HashMap<>();

        for (UsageSignal signal : scopedSignals) {
            Long signalSubscriptionId = signal.getSubscription().getId();
            signalsCountBySubscription.merge(signalSubscriptionId, 1L, Long::sum);

            OffsetDateTime previousLast = lastSignalAtBySubscription.get(signalSubscriptionId);
            if (previousLast == null || signal.getCreatedAt().isAfter(previousLast)) {
                lastSignalAtBySubscription.put(signalSubscriptionId, signal.getCreatedAt());
            }
        }

        List<AnalyticsUsageItemResponse> items = subscriptionsInScope.stream()
                .map(subscription -> {
                    long signalsCount = signalsCountBySubscription.getOrDefault(subscription.getId(), 0L);
                    OffsetDateTime lastSignalAt = lastSignalAtBySubscription.get(subscription.getId());

                    return new AnalyticsUsageItemResponse(
                            subscription.getId(),
                            subscription.getServiceName(),
                            resolveCategory(subscription),
                            signalsCount,
                            lastSignalAt == null ? null : lastSignalAt.toString()
                    );
                })
                .sorted(Comparator.comparingLong(AnalyticsUsageItemResponse::signalsCount)
                        .reversed()
                        .thenComparing(item -> item.serviceName() == null ? "" : item.serviceName()))
                .toList();

        long totalSignals = scopedSignals.size();
        long activeSubscriptionsCount = subscriptionsInScope.size();
        long subscriptionsWithSignals = items.stream()
                .filter(item -> item.signalsCount() > 0)
                .count();

        return new AnalyticsUsageResponse(
                period.apiValue(),
                from,
                to,
                totalSignals,
                activeSubscriptionsCount,
                subscriptionsWithSignals,
                items
        );
    }

    private List<SubscriptionSpend> calculateSpendsForRange(Long userId, LocalDate from, LocalDate to) {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        List<SubscriptionSpend> spends = new ArrayList<>();

        for (Subscription subscription : activeSubscriptions) {
            long occurrences = occurrencesInRange(subscription, from, to);
            if (occurrences <= 0) {
                continue;
            }

            BigDecimal amountForRange = money(subscription.getAmount().multiply(BigDecimal.valueOf(occurrences)));
            spends.add(new SubscriptionSpend(subscription, amountForRange));
        }

        return spends;
    }

    private long occurrencesInRange(Subscription subscription, LocalDate from, LocalDate to) {
        LocalDate billingDate = subscription.getNextBillingDate();

        while (billingDate.isBefore(from)) {
            billingDate = nextDate(billingDate, subscription.getBillingPeriod());
        }

        long count = 0;
        while (!billingDate.isAfter(to)) {
            count++;
            billingDate = nextDate(billingDate, subscription.getBillingPeriod());
        }

        return count;
    }

    private LocalDate nextDate(LocalDate current, BillingPeriod billingPeriod) {
        return switch (billingPeriod) {
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
        };
    }

    private BigDecimal totalAmount(List<SubscriptionSpend> spends) {
        return spends.stream()
                .map(SubscriptionSpend::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return amount.multiply(HUNDRED)
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveCurrency(List<SubscriptionSpend> spends) {
        Set<String> currencies = spends.stream()
                .map(spend -> spend.subscription().getCurrency())
                .filter(Objects::nonNull)
                .map(currency -> currency.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (currencies.isEmpty()) {
            return null;
        }

        if (currencies.size() == 1) {
            return currencies.iterator().next();
        }

        return MIXED_CURRENCY;
    }

    private String resolveCategory(Subscription subscription) {
        if (subscription.getCategory() == null || subscription.getCategory().getName() == null || subscription.getCategory().getName().isBlank()) {
            return UNCATEGORIZED;
        }

        return subscription.getCategory().getName();
    }

    private void validateSubscriptionId(Long subscriptionId) {
        if (subscriptionId <= 0) {
            throw new ApiException("subscriptionId must be greater than 0");
        }
    }

    private OffsetDateTime rangeStartDateTime(LocalDate from) {
        return from.atStartOfDay().atOffset(ANALYTICS_ZONE_OFFSET);
    }

    private OffsetDateTime rangeEndDateTime(LocalDate to) {
        return to.plusDays(1)
                .atStartOfDay()
                .atOffset(ANALYTICS_ZONE_OFFSET)
                .minusNanos(1);
    }

    private record SubscriptionSpend(Subscription subscription, BigDecimal amount) {
    }
}
