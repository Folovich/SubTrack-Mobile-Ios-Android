package com.subscriptionmanager.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsUsageResponse(
        String period,
        LocalDate from,
        LocalDate to,
        long totalSignals,
        long activeSubscriptions,
        long subscriptionsWithSignals,
        List<AnalyticsUsageItemResponse> items
) {
}
