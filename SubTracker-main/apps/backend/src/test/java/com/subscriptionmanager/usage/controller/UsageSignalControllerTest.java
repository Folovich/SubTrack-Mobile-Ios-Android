package com.subscriptionmanager.usage.controller;

import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.usage.dto.UsageSignalCreateRequest;
import com.subscriptionmanager.usage.dto.UsageSignalResponse;
import com.subscriptionmanager.usage.service.UsageSignalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsageSignalControllerTest {

    private UsageSignalService usageSignalService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usageSignalService = Mockito.mock(UsageSignalService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        UsageSignalController controller = new UsageSignalController(usageSignalService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsUsageSignalShape() throws Exception {
        String body = """
                {
                  "subscriptionId": 123,
                  "signalType": "CONTENT_WATCHED",
                  "value": "episode=3"
                }
                """;

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(usageSignalService.create(eq(1L), any(UsageSignalCreateRequest.class)))
                .thenReturn(new UsageSignalResponse(
                        1L,
                        123L,
                        "CONTENT_WATCHED",
                        "episode=3",
                        "2026-03-12T10:15:30Z"
                ));

        mockMvc.perform(post("/api/v1/usage-signals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subscriptionId").value(123))
                .andExpect(jsonPath("$.signalType").value("CONTENT_WATCHED"))
                .andExpect(jsonPath("$.value").value("episode=3"))
                .andExpect(jsonPath("$.createdAt").value("2026-03-12T10:15:30Z"));

        verify(usageSignalService).create(eq(1L), any(UsageSignalCreateRequest.class));
    }

    @Test
    void listReturnsUsageSignalsShape() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(usageSignalService.list(1L, 123L))
                .thenReturn(List.of(new UsageSignalResponse(
                        2L,
                        123L,
                        "APP_OPENED",
                        "session=morning",
                        "2026-03-12T11:00:00Z"
                )));

        mockMvc.perform(get("/api/v1/usage-signals").queryParam("subscriptionId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].subscriptionId").value(123))
                .andExpect(jsonPath("$[0].signalType").value("APP_OPENED"))
                .andExpect(jsonPath("$[0].value").value("session=morning"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-12T11:00:00Z"));
    }

    @Test
    void createReturnsValidationErrorsForInvalidPayload() throws Exception {
        String body = """
                {
                  "subscriptionId": 0,
                  "signalType": "",
                  "value": ""
                }
                """;

        mockMvc.perform(post("/api/v1/usage-signals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.subscriptionId").value("subscriptionId must be greater than 0"))
                .andExpect(jsonPath("$.errors.signalType").value("signalType is required"))
                .andExpect(jsonPath("$.errors.value").value("value is required"));
    }

    @Test
    void createReturnsBadRequestWhenSubscriptionNotOwned() throws Exception {
        String body = """
                {
                  "subscriptionId": 999,
                  "signalType": "CONTENT_WATCHED",
                  "value": "episode=3"
                }
                """;

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(usageSignalService.create(eq(1L), any(UsageSignalCreateRequest.class)))
                .thenThrow(new ApiException("Subscription not found"));

        mockMvc.perform(post("/api/v1/usage-signals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subscription not found"));
    }
}
