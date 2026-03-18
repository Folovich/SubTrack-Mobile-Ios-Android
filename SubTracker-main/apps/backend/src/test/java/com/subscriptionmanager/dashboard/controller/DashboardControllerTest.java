package com.subscriptionmanager.dashboard.controller;

import com.subscriptionmanager.analytics.dto.AnalyticsByCategoryResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsForecastResponse;
import com.subscriptionmanager.analytics.dto.AnalyticsSummaryResponse;
import com.subscriptionmanager.dashboard.dto.DashboardResponse;
import com.subscriptionmanager.dashboard.service.DashboardService;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private DashboardService dashboardService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dashboardService = Mockito.mock(DashboardService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        DashboardController controller = new DashboardController(dashboardService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getWithoutParamsUsesDefaultPeriodAndDays() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(dashboardService.get(1L, "month", 7)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.period").value("month"));

        verify(dashboardService).get(1L, "month", 7);
    }

    @Test
    void getReturnsBadRequestForInvalidPeriod() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(dashboardService.get(1L, "week", 7))
                .thenThrow(new ApiException("period must be one of: month, year"));

        mockMvc.perform(get("/api/v1/dashboard").queryParam("period", "week"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("period must be one of: month, year"));
    }

    @Test
    void getReturnsBadRequestForInvalidDays() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(dashboardService.get(1L, "month", 0))
                .thenThrow(new ApiException("days must be between 1 and 365"));

        mockMvc.perform(get("/api/v1/dashboard").queryParam("days", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("days must be between 1 and 365"));
    }

    private DashboardResponse sampleResponse() {
        return new DashboardResponse(
                new AnalyticsSummaryResponse(
                        "month",
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31),
                        BigDecimal.valueOf(10).setScale(2),
                        1,
                        "USD"
                ),
                new AnalyticsForecastResponse(
                        BigDecimal.valueOf(10).setScale(2),
                        BigDecimal.valueOf(120).setScale(2),
                        "USD"
                ),
                new AnalyticsByCategoryResponse(
                        "month",
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 31),
                        BigDecimal.valueOf(10).setScale(2),
                        "USD",
                        List.of()
                ),
                List.of(),
                List.of()
        );
    }
}
