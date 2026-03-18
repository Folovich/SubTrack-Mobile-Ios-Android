package com.subscriptionmanager.subscription.controller;

import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.exception.SubscriptionNotFoundException;
import com.subscriptionmanager.exception.SubscriptionOwnershipException;
import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.subscription.dto.SupportEmailDraftDetailsResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailDraftResponse;
import com.subscriptionmanager.subscription.service.SubscriptionService;
import com.subscriptionmanager.subscription.support.SupportEmailAction;
import com.subscriptionmanager.subscription.support.SupportEmailDraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionSupportEmailControllerTest {

    private SubscriptionService subscriptionService;
    private CurrentUserService currentUserService;
    private SupportEmailDraftService supportEmailDraftService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        subscriptionService = Mockito.mock(SubscriptionService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        supportEmailDraftService = Mockito.mock(SupportEmailDraftService.class);

        SubscriptionController controller = new SubscriptionController(
                subscriptionService,
                currentUserService,
                supportEmailDraftService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void supportDraftReturnsOk() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(supportEmailDraftService.getDraft(1L, 42L, SupportEmailAction.CANCEL))
                .thenReturn(new SupportEmailDraftResponse(
                        42L,
                        SupportEmailAction.CANCEL,
                        "GMAIL",
                        new SupportEmailDraftDetailsResponse(
                                "support@netflix.com",
                                "Cancel my Netflix subscription",
                                "Hello, please cancel.",
                                "mailto:support@netflix.com?subject=Cancel&body=Hello",
                                "Hello, please cancel."
                        )
                ));

        mockMvc.perform(get("/api/v1/subscriptions/42/support-email-draft")
                        .param("action", "CANCEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(42))
                .andExpect(jsonPath("$.action").value("CANCEL"))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.draft.to").value("support@netflix.com"))
                .andExpect(jsonPath("$.draft.mailtoUrl").value("mailto:support@netflix.com?subject=Cancel&body=Hello"));
    }

    @Test
    void supportDraftReturnsBadRequestForInvalidAction() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/42/support-email-draft")
                        .param("action", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: action"));
    }

    @Test
    void supportDraftReturnsBadRequestWhenActionIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/42/support-email-draft"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: action"));
    }

    @Test
    void supportDraftReturnsForbiddenWhenSubscriptionBelongsToAnotherUser() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(supportEmailDraftService.getDraft(1L, 42L, SupportEmailAction.CANCEL))
                .thenThrow(new SubscriptionOwnershipException("Subscription does not belong to current user"));

        mockMvc.perform(get("/api/v1/subscriptions/42/support-email-draft")
                        .param("action", "CANCEL"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Subscription does not belong to current user"));
    }

    @Test
    void supportDraftReturnsNotFoundWhenMissing() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(supportEmailDraftService.getDraft(1L, 42L, SupportEmailAction.CANCEL))
                .thenThrow(new SubscriptionNotFoundException("Subscription not found"));

        mockMvc.perform(get("/api/v1/subscriptions/42/support-email-draft")
                        .param("action", "CANCEL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subscription not found"));
    }

    @Test
    void supportEmailEventReturnsNoContent() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);

        mockMvc.perform(post("/api/v1/subscriptions/42/support-email-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action":"CANCEL",
                                  "event":"TEXT_COPIED"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(supportEmailDraftService).trackEvent(eq(1L), eq(42L), any());
    }
}
