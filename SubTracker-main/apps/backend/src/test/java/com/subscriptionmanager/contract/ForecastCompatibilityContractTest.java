package com.subscriptionmanager.contract;

import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.forecast.controller.ForecastController;
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

class ForecastCompatibilityContractTest {

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
    void forecastAliasContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(forecastService.forecast(1L, 12))
                .thenReturn(new ForecastResponse(
                        BigDecimal.valueOf(44.50),
                        BigDecimal.valueOf(534.00)
                ));

        mockMvc.perform(get("/api/v1/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyForecast").value(44.50))
                .andExpect(jsonPath("$.yearlyForecast").value(534.00));

        verify(forecastService).forecast(1L, 12);
    }
}
