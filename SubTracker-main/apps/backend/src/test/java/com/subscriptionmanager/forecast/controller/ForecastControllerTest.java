package com.subscriptionmanager.forecast.controller;

import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.forecast.dto.ForecastResponse;
import com.subscriptionmanager.forecast.service.ForecastService;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ForecastControllerTest {

    private ForecastService forecastService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        forecastService = Mockito.mock(ForecastService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        ForecastController controller = new ForecastController(forecastService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void forecastReturnsNonZeroValues() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(forecastService.forecast(1L, 6))
                .thenReturn(new ForecastResponse(
                        BigDecimal.valueOf(29.99).setScale(2),
                        BigDecimal.valueOf(359.88).setScale(2)
                ));

        mockMvc.perform(get("/api/v1/forecast").queryParam("months", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyForecast").value(29.99))
                .andExpect(jsonPath("$.yearlyForecast").value(359.88));

        verify(forecastService).forecast(1L, 6);
    }

    @Test
    void forecastUsesDefaultMonthsWhenParamMissing() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(forecastService.forecast(1L, 12))
                .thenReturn(new ForecastResponse(
                        BigDecimal.valueOf(10).setScale(2),
                        BigDecimal.valueOf(120).setScale(2)
                ));

        mockMvc.perform(get("/api/v1/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyForecast").value(10.00))
                .andExpect(jsonPath("$.yearlyForecast").value(120.00));

        verify(forecastService).forecast(1L, 12);
    }
}
