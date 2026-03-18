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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;

    private final AnalyticsService analyticsService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    public DashboardServiceImpl(
            AnalyticsService analyticsService,
            SubscriptionService subscriptionService,
            NotificationService notificationService
    ) {
        this.analyticsService = analyticsService;
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
    }

    @Override
    public DashboardResponse get(Long userId, String period, int days) {
        AnalyticsPeriod parsedPeriod = AnalyticsPeriod.from(period);
        validateDays(days);

        AnalyticsSummaryResponse summary = analyticsService.summary(userId, parsedPeriod);
        AnalyticsForecastResponse forecast = analyticsService.forecast(userId);
        AnalyticsByCategoryResponse byCategory = analyticsService.byCategory(userId, parsedPeriod);
        List<UpcomingSubscriptionResponse> upcoming = subscriptionService.upcoming(userId, days);
        List<NotificationResponse> notifications = notificationService.list(userId, days);

        return new DashboardResponse(summary, forecast, byCategory, upcoming, notifications);
    }

    private void validateDays(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new ApiException("days must be between 1 and 365");
        }
    }
}
