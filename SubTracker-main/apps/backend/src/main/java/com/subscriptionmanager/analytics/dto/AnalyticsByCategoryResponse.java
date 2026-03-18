package com.subscriptionmanager.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsByCategoryResponse(
        String period,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        String currency,
        List<AnalyticsCategoryItemResponse> items
) {
}
