package com.subscriptionmanager.analytics.service;

import com.subscriptionmanager.analytics.AnalyticsPeriod;
import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsCategoryItemResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsUsageResponse;
import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Category;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.UsageSignal;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UsageSignalRepository usageSignalRepository;

    @Test
    void summaryCountsOnlyActiveSubscriptionsWithChargesInCalendarMonth() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();

        Subscription inRange = subscription(
                1L,
                "Netflix",
                BigDecimal.valueOf(12.99),
                "USD",
                BillingPeriod.MONTHLY,
                today.withDayOfMonth(1),
                SubscriptionStatus.ACTIVE,
                category("Entertainment")
        );
        Subscription outOfRange = subscription(
                2L,
                "Yearly Tool",
                BigDecimal.valueOf(100),
                "USD",
                BillingPeriod.YEARLY,
                today.plusMonths(1),
                SubscriptionStatus.ACTIVE,
                category("Productivity")
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(inRange, outOfRange));

        AnalyticsSummaryResponse response = service.summary(1L, AnalyticsPeriod.MONTH);

        assertEquals("month", response.period());
        assertEquals(BigDecimal.valueOf(12.99).setScale(2), response.totalAmount());
        assertEquals(1, response.activeSubscriptions());
        assertEquals("USD", response.currency());
    }

    @Test
    void byCategoryRoundsSharePercentAndUsesUncategorized() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();

        Subscription entertainment = subscription(
                1L,
                "Netflix",
                BigDecimal.valueOf(10),
                "USD",
                BillingPeriod.MONTHLY,
                today.withDayOfMonth(1),
                SubscriptionStatus.ACTIVE,
                category("Entertainment")
        );
        Subscription uncategorized = subscription(
                2L,
                "Unknown",
                BigDecimal.valueOf(30),
                "USD",
                BillingPeriod.MONTHLY,
                today.withDayOfMonth(1),
                SubscriptionStatus.ACTIVE,
                null
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(entertainment, uncategorized));

        AnalyticsByCategoryResponse response = service.byCategory(1L, AnalyticsPeriod.MONTH);

        assertEquals(BigDecimal.valueOf(40).setScale(2), response.totalAmount());
        assertEquals(2, response.items().size());

        AnalyticsCategoryItemResponse first = response.items().getFirst();
        AnalyticsCategoryItemResponse second = response.items().get(1);

        assertEquals("Uncategorized", first.category());
        assertEquals(BigDecimal.valueOf(75.00).setScale(2), first.sharePercent());
        assertEquals("Entertainment", second.category());
        assertEquals(BigDecimal.valueOf(25.00).setScale(2), second.sharePercent());
    }

    @Test
    void forecastReturnsMonthAndYearTotalsForCalendarRanges() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();

        Subscription yearly = subscription(
                1L,
                "Insurance",
                BigDecimal.valueOf(50),
                "USD",
                BillingPeriod.YEARLY,
                today,
                SubscriptionStatus.ACTIVE,
                category("Finance")
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(yearly));

        AnalyticsForecastResponse response = service.forecast(1L);

        assertEquals(BigDecimal.valueOf(50).setScale(2), response.monthForecast());
        assertEquals(BigDecimal.valueOf(50).setScale(2), response.yearForecast());
        assertEquals("USD", response.currency());
    }

    @Test
    void byCategoryReturnsEmptyItemsWhenNothingFallsIntoRange() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();

        Subscription nextYear = subscription(
                1L,
                "Annual",
                BigDecimal.valueOf(100),
                "USD",
                BillingPeriod.YEARLY,
                today.plusYears(1),
                SubscriptionStatus.ACTIVE,
                category("Finance")
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(nextYear));

        AnalyticsByCategoryResponse response = service.byCategory(1L, AnalyticsPeriod.MONTH);

        assertEquals(BigDecimal.ZERO.setScale(2), response.totalAmount());
        assertEquals(0, response.items().size());
    }

    @Test
    void usageAggregatesSignalsBySubscriptionAndSortsBySignalsCountDescThenServiceNameAsc() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();

        Subscription alpha = subscription(
                11L,
                "Alpha Stream",
                BigDecimal.valueOf(10),
                "USD",
                BillingPeriod.MONTHLY,
                today,
                SubscriptionStatus.ACTIVE,
                category("Entertainment")
        );
        Subscription beta = subscription(
                12L,
                "Beta Notes",
                BigDecimal.valueOf(7),
                "USD",
                BillingPeriod.MONTHLY,
                today,
                SubscriptionStatus.ACTIVE,
                null
        );
        Subscription gamma = subscription(
                13L,
                "Gamma Learn",
                BigDecimal.valueOf(5),
                "USD",
                BillingPeriod.MONTHLY,
                today,
                SubscriptionStatus.ACTIVE,
                category("Education")
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(alpha, beta, gamma));
        when(usageSignalRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        usageSignal(11L, "2026-03-05T10:00:00Z"),
                        usageSignal(11L, "2026-03-07T12:00:00Z"),
                        usageSignal(12L, "2026-03-06T08:00:00Z")
                ));

        AnalyticsUsageResponse response = service.usage(1L, AnalyticsPeriod.MONTH, null);

        assertEquals(3, response.totalSignals());
        assertEquals(3, response.activeSubscriptions());
        assertEquals(2, response.subscriptionsWithSignals());
        assertEquals(3, response.items().size());

        assertEquals(11L, response.items().get(0).subscriptionId());
        assertEquals(2L, response.items().get(0).signalsCount());
        assertEquals("2026-03-07T12:00Z", response.items().get(0).lastSignalAt());

        assertEquals(12L, response.items().get(1).subscriptionId());
        assertEquals("Uncategorized", response.items().get(1).category());

        assertEquals(13L, response.items().get(2).subscriptionId());
        assertEquals(0L, response.items().get(2).signalsCount());
        assertNull(response.items().get(2).lastSignalAt());
    }

    @Test
    void usageUsesMonthBoundariesWhenPeriodMonth() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        when(usageSignalRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        AnalyticsUsageResponse response = service.usage(1L, AnalyticsPeriod.MONTH, null);

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(usageSignalRepository).findByUserIdAndCreatedAtBetween(eq(1L), fromCaptor.capture(), toCaptor.capture());

        assertEquals(monthStart, response.from());
        assertEquals(monthEnd, response.to());
        assertEquals(monthStart.atStartOfDay().atOffset(ZoneOffset.UTC), fromCaptor.getValue());
        assertEquals(
                monthEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1),
                toCaptor.getValue()
        );
    }

    @Test
    void usageUsesYearBoundariesWhenPeriodYear() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate yearEnd = today.withDayOfYear(today.lengthOfYear());

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        when(usageSignalRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        AnalyticsUsageResponse response = service.usage(1L, AnalyticsPeriod.YEAR, null);

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(usageSignalRepository).findByUserIdAndCreatedAtBetween(eq(1L), fromCaptor.capture(), toCaptor.capture());

        assertEquals(yearStart, response.from());
        assertEquals(yearEnd, response.to());
        assertEquals(yearStart.atStartOfDay().atOffset(ZoneOffset.UTC), fromCaptor.getValue());
        assertEquals(
                yearEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1),
                toCaptor.getValue()
        );
    }

    @Test
    void usageReturnsEmptyMetricsWhenNoActiveSubscriptionsAndNoSignals() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        when(usageSignalRepository.findByUserIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        AnalyticsUsageResponse response = service.usage(1L, AnalyticsPeriod.MONTH, null);

        assertEquals(0, response.totalSignals());
        assertEquals(0, response.activeSubscriptions());
        assertEquals(0, response.subscriptionsWithSignals());
        assertEquals(0, response.items().size());
    }

    @Test
    void usageAppliesSubscriptionIdFilter() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        LocalDate today = LocalDate.now();

        Subscription alpha = subscription(
                21L,
                "Alpha Stream",
                BigDecimal.valueOf(10),
                "USD",
                BillingPeriod.MONTHLY,
                today,
                SubscriptionStatus.ACTIVE,
                category("Entertainment")
        );
        Subscription beta = subscription(
                22L,
                "Beta Notes",
                BigDecimal.valueOf(10),
                "USD",
                BillingPeriod.MONTHLY,
                today,
                SubscriptionStatus.ACTIVE,
                category("Productivity")
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(alpha, beta));
        when(subscriptionRepository.findByIdAndUserId(22L, 1L)).thenReturn(Optional.of(beta));
        when(usageSignalRepository.findByUserIdAndSubscriptionIdAndCreatedAtBetween(eq(1L), eq(22L), any(), any()))
                .thenReturn(List.of(
                        usageSignal(22L, "2026-03-06T08:00:00Z"),
                        usageSignal(22L, "2026-03-07T08:00:00Z")
                ));

        AnalyticsUsageResponse response = service.usage(1L, AnalyticsPeriod.MONTH, 22L);

        assertEquals(2, response.totalSignals());
        assertEquals(1, response.activeSubscriptions());
        assertEquals(1, response.subscriptionsWithSignals());
        assertEquals(1, response.items().size());
        assertEquals(22L, response.items().getFirst().subscriptionId());
    }

    @Test
    void usageThrowsWhenSubscriptionNotFoundOrNotOwned() {
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        when(subscriptionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.usage(1L, AnalyticsPeriod.MONTH, 999L));

        assertEquals("Subscription not found", exception.getMessage());
    }

    private Subscription subscription(
            Long id,
            String serviceName,
            BigDecimal amount,
            String currency,
            BillingPeriod billingPeriod,
            LocalDate nextBillingDate,
            SubscriptionStatus status,
            Category category
    ) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setServiceName(serviceName);
        subscription.setAmount(amount);
        subscription.setCurrency(currency);
        subscription.setBillingPeriod(billingPeriod);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setStatus(status);
        subscription.setCategory(category);
        return subscription;
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    private UsageSignal usageSignal(Long subscriptionId, String createdAt) {
        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);

        UsageSignal usageSignal = new UsageSignal();
        usageSignal.setSubscription(subscription);
        usageSignal.setCreatedAt(OffsetDateTime.parse(createdAt));
        return usageSignal;
    }
}
