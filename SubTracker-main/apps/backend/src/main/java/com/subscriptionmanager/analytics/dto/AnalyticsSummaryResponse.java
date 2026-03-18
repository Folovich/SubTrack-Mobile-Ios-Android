package com.subscriptionmanager.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalyticsSummaryResponse(
        String period,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long activeSubscriptions,
        String currency
) {
}
