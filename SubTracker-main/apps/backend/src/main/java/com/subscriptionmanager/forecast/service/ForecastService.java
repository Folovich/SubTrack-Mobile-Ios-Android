package com.subscriptionmanager.forecast.service;

import com.subscriptionmanager.forecast.dto.ForecastResponse;

public interface ForecastService {
    ForecastResponse forecast(Long userId, int months);
}
