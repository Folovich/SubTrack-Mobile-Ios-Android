package com.subscriptionmanager.analytics;

import com.subscriptionmanager.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyticsPeriodTest {

    @Test
    void fromAcceptsMonthAndYearIgnoringCase() {
        assertEquals(AnalyticsPeriod.MONTH, AnalyticsPeriod.from("month"));
        assertEquals(AnalyticsPeriod.YEAR, AnalyticsPeriod.from("YEAR"));
    }

    @Test
    void fromThrowsForInvalidValue() {
        assertThrows(ApiException.class, () -> AnalyticsPeriod.from("week"));
        assertThrows(ApiException.class, () -> AnalyticsPeriod.from(null));
    }

    @Test
    void rangeUsesCalendarBoundaries() {
        LocalDate date = LocalDate.of(2026, 3, 8);

        assertEquals(LocalDate.of(2026, 3, 1), AnalyticsPeriod.MONTH.rangeStart(date));
        assertEquals(LocalDate.of(2026, 3, 31), AnalyticsPeriod.MONTH.rangeEnd(date));
        assertEquals(LocalDate.of(2026, 1, 1), AnalyticsPeriod.YEAR.rangeStart(date));
        assertEquals(LocalDate.of(2026, 12, 31), AnalyticsPeriod.YEAR.rangeEnd(date));
    }
}
