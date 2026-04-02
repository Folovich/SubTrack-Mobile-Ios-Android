package com.subscriptionmanager.analytics.dto;

public record AnalyticsUsageItemResponse(
        Long subscriptionId,
        String serviceName,
        String category,
        long signalsCount,
        String lastSignalAt
) {
}
