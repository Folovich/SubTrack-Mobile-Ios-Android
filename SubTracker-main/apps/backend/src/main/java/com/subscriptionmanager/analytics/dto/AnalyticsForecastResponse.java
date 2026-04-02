package com.subscriptionmanager.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsForecastResponse(
        BigDecimal monthForecast,
        BigDecimal yearForecast,
        String currency
) {
}
