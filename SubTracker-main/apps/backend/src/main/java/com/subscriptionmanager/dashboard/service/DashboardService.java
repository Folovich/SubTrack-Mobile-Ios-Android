package com.subscriptionmanager.dashboard.service;

import com.subscriptionmanager.dashboard.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse get(Long userId, String period, int days);
}
