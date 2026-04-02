package com.subscriptionmanager.dashboard.dto;

import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.notification.dto.NotificationResponse;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;

import java.util.List;

public record DashboardResponse(
        AnalyticsSummaryResponse summary,
        AnalyticsForecastResponse forecast,
        AnalyticsByCategoryResponse byCategory,
        List<UpcomingSubscriptionResponse> upcoming,
        List<NotificationResponse> notifications
) {
}
