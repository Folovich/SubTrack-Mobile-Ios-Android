package com.subscriptionmanager.forecast.controller;

import com.subscriptionmanager.forecast.dto.ForecastResponse;
import com.subscriptionmanager.forecast.service.ForecastService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forecast")
@Deprecated(since = "0.1.0", forRemoval = false)
public class ForecastController {

    private final ForecastService forecastService;
    private final CurrentUserService currentUserService;

    public ForecastController(ForecastService forecastService, CurrentUserService currentUserService) {
        this.forecastService = forecastService;
        this.currentUserService = currentUserService;
    }

    /**
     * @deprecated Use /api/v1/analytics/forecast. This endpoint is kept as a compatibility alias.
     */
    @GetMapping
    @Deprecated(since = "0.1.0", forRemoval = false)
    public ForecastResponse forecast(@RequestParam(defaultValue = "12") int months) {
        Long userId = currentUserService.requireCurrentUserId();
        return forecastService.forecast(userId, months);
    }
}
