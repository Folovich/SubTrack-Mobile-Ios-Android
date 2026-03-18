package com.subscriptionmanager.analytics;

import com.subscriptionmanager.exception.ApiException;

import java.time.LocalDate;
import java.util.Locale;

public enum AnalyticsPeriod {
    MONTH,
    YEAR;

    public static AnalyticsPeriod from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ApiException("period is required");
        }

        return switch (rawValue.trim().toUpperCase(Locale.ROOT)) {
            case "MONTH" -> MONTH;
            case "YEAR" -> YEAR;
            default -> throw new ApiException("period must be one of: month, year");
        };
    }

    public LocalDate rangeStart(LocalDate today) {
        return switch (this) {
            case MONTH -> today.withDayOfMonth(1);
            case YEAR -> today.withDayOfYear(1);
        };
    }

    public LocalDate rangeEnd(LocalDate today) {
        return switch (this) {
            case MONTH -> today.withDayOfMonth(today.lengthOfMonth());
            case YEAR -> today.withDayOfYear(today.lengthOfYear());
        };
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
