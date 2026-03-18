package com.subscriptionmanager.analytics.service;

import com.subscriptionmanager.analytics.AnalyticsPeriod;
import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsUsageResponse;

public interface AnalyticsService {
    AnalyticsSummaryResponse summary(Long userId, AnalyticsPeriod period);
    AnalyticsByCategoryResponse byCategory(Long userId, AnalyticsPeriod period);
    AnalyticsForecastResponse forecast(Long userId);
    AnalyticsUsageResponse usage(Long userId, AnalyticsPeriod period, Long subscriptionId);
}
