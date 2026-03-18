package com.subscriptionmanager.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsCategoryItemResponse(
        String category,
        BigDecimal amount,
        BigDecimal sharePercent,
        int subscriptionsCount
) {
}
