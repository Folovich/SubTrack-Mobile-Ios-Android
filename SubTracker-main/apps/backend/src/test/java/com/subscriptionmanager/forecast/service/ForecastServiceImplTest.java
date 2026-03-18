package com.subscriptionmanager.forecast.service;

import com.subscriptionmanager.analytics.service.AnalyticsServiceImpl;
import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.forecast.dto.ForecastResponse;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UsageSignalRepository usageSignalRepository;

    @Test
    void forecastUsesAnalyticsCalculationAndReturnsNonZeroValues() {
        AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        ForecastServiceImpl forecastService = new ForecastServiceImpl(analyticsService);

        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        Subscription monthly = subscription(
                BigDecimal.TEN,
                "USD",
                BillingPeriod.MONTHLY,
                yearStart,
                SubscriptionStatus.ACTIVE
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(monthly));

        ForecastResponse response = forecastService.forecast(1L, 12);

        assertEquals(BigDecimal.valueOf(10).setScale(2), response.monthlyForecast());
        assertEquals(BigDecimal.valueOf(120).setScale(2), response.yearlyForecast());
    }

    @Test
    void forecastIsConsistentForAnyMonthsParameter() {
        AnalyticsServiceImpl analyticsService = new AnalyticsServiceImpl(subscriptionRepository, usageSignalRepository);
        ForecastServiceImpl forecastService = new ForecastServiceImpl(analyticsService);

        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        Subscription monthly = subscription(
                BigDecimal.valueOf(15),
                "USD",
                BillingPeriod.MONTHLY,
                yearStart,
                SubscriptionStatus.ACTIVE
        );

        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(monthly));

        ForecastResponse oneMonth = forecastService.forecast(1L, 1);
        ForecastResponse twentyFourMonths = forecastService.forecast(1L, 24);

        assertEquals(oneMonth, twentyFourMonths);
        assertEquals(BigDecimal.valueOf(15).setScale(2), oneMonth.monthlyForecast());
        assertEquals(BigDecimal.valueOf(180).setScale(2), oneMonth.yearlyForecast());
    }

    private Subscription subscription(
            BigDecimal amount,
            String currency,
            BillingPeriod billingPeriod,
            LocalDate nextBillingDate,
            SubscriptionStatus status
    ) {
        Subscription subscription = new Subscription();
        subscription.setAmount(amount);
        subscription.setCurrency(currency);
        subscription.setBillingPeriod(billingPeriod);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setStatus(status);
        return subscription;
    }
}
