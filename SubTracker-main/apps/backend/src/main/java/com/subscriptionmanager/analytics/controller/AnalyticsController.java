package com.subscriptionmanager.analytics.controller;

import com.subscriptionmanager.analytics.AnalyticsPeriod;
import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsUsageResponse;
import com.subscriptionmanager.analytics.service.AnalyticsService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse summary(@RequestParam String period) {
        Long userId = currentUserService.requireCurrentUserId();
        return analyticsService.summary(userId, AnalyticsPeriod.from(period));
    }

    @GetMapping("/by-category")
    public AnalyticsByCategoryResponse byCategory(@RequestParam String period) {
        Long userId = currentUserService.requireCurrentUserId();
        return analyticsService.byCategory(userId, AnalyticsPeriod.from(period));
    }

    @GetMapping("/forecast")
    public AnalyticsForecastResponse forecast() {
        Long userId = currentUserService.requireCurrentUserId();
        return analyticsService.forecast(userId);
    }

    @GetMapping("/usage")
    public AnalyticsUsageResponse usage(
            @RequestParam String period,
            @RequestParam(required = false) Long subscriptionId
    ) {
        Long userId = currentUserService.requireCurrentUserId();
        return analyticsService.usage(userId, AnalyticsPeriod.from(period), subscriptionId);
    }
}
