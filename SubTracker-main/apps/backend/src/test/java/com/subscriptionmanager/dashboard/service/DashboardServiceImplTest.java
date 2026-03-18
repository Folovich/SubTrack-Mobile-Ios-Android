package com.subscriptionmanager.dashboard.service;

import com.subscriptionmanager.analytics.AnalyticsPeriod;
import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.analytics.service.AnalyticsService;
import com.subscriptionmanager.dashboard.dto.DashboardResponse;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.notification.dto.NotificationResponse;
import com.subscriptionmanager.notification.service.NotificationService;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;
import com.subscriptionmanager.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private NotificationService notificationService;

    @Test
    void getAggregatesSummaryForecastCategoryUpcomingAndNotifications() {
        DashboardServiceImpl service = new DashboardServiceImpl(analyticsService, subscriptionService, notificationService);

        AnalyticsSummaryResponse summary = new AnalyticsSummaryResponse(
                "month",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(29.99).setScale(2),
                2,
                "USD"
        );
        AnalyticsForecastResponse forecast = new AnalyticsForecastResponse(
                BigDecimal.valueOf(29.99).setScale(2),
                BigDecimal.valueOf(359.88).setScale(2),
                "USD"
        );
        AnalyticsByCategoryResponse byCategory = new AnalyticsByCategoryResponse(
                "month",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(29.99).setScale(2),
                "USD",
                List.of()
        );
        List<UpcomingSubscriptionResponse> upcoming = List.of(
                new UpcomingSubscriptionResponse(
                        1L,
                        "Netflix",
                        "Entertainment",
                        BigDecimal.valueOf(9.99).setScale(2),
                        "USD",
                        com.subscriptionmanager.common.enums.BillingPeriod.MONTHLY,
                        LocalDate.of(2026, 3, 10),
                        com.subscriptionmanager.common.enums.SubscriptionStatus.ACTIVE,
                        2
                )
        );
        List<NotificationResponse> notifications = List.of(
                new NotificationResponse(11L, "UPCOMING_CHARGE", "Netflix reminder", "2026-03-08T09:00:00Z", "PENDING")
        );

        when(analyticsService.summary(1L, AnalyticsPeriod.MONTH)).thenReturn(summary);
        when(analyticsService.forecast(1L)).thenReturn(forecast);
        when(analyticsService.byCategory(1L, AnalyticsPeriod.MONTH)).thenReturn(byCategory);
        when(subscriptionService.upcoming(1L, 7)).thenReturn(upcoming);
        when(notificationService.list(1L, 7)).thenReturn(notifications);

        DashboardResponse response = service.get(1L, "month", 7);

        assertEquals(summary, response.summary());
        assertEquals(forecast, response.forecast());
        assertEquals(byCategory, response.byCategory());
        assertEquals(upcoming, response.upcoming());
        assertEquals(notifications, response.notifications());

        verify(analyticsService).summary(1L, AnalyticsPeriod.MONTH);
        verify(analyticsService).forecast(1L);
        verify(analyticsService).byCategory(1L, AnalyticsPeriod.MONTH);
        verify(subscriptionService).upcoming(1L, 7);
        verify(notificationService).list(1L, 7);
    }

    @Test
    void getThrowsForInvalidPeriod() {
        DashboardServiceImpl service = new DashboardServiceImpl(analyticsService, subscriptionService, notificationService);

        assertThrows(ApiException.class, () -> service.get(1L, "week", 7));

        verifyNoInteractions(analyticsService, subscriptionService, notificationService);
    }

    @Test
    void getThrowsForInvalidDays() {
        DashboardServiceImpl service = new DashboardServiceImpl(analyticsService, subscriptionService, notificationService);

        assertThrows(ApiException.class, () -> service.get(1L, "month", 0));

        verifyNoInteractions(analyticsService, subscriptionService, notificationService);
    }
}
