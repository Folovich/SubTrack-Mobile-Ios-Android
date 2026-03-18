package com.subscriptionmanager.dashboard.controller;

import com.subscriptionmanager.dashboard.dto.DashboardResponse;
import com.subscriptionmanager.dashboard.service.DashboardService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public DashboardResponse get(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "7") int days
    ) {
        Long userId = currentUserService.requireCurrentUserId();
        return dashboardService.get(userId, period, days);
    }
}
