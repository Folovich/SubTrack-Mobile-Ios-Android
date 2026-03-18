package com.subscriptionmanager.forecast.service;

import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.service.AnalyticsService;
import com.subscriptionmanager.forecast.dto.ForecastResponse;
import org.springframework.stereotype.Service;

@Service
public class ForecastServiceImpl implements ForecastService {

    private final AnalyticsService analyticsService;

    public ForecastServiceImpl(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public ForecastResponse forecast(Long userId, int months) {
        AnalyticsForecastResponse analyticsForecast = analyticsService.forecast(userId);
        return new ForecastResponse(analyticsForecast.monthForecast(), analyticsForecast.yearForecast());
    }
}
