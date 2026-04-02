package com.subscriptionmanager.forecast.dto;

import java.math.BigDecimal;

public record ForecastResponse(BigDecimal monthlyForecast, BigDecimal yearlyForecast) {
}
