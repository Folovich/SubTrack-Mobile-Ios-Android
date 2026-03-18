package com.subscriptionmanager.contract;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.subscription.controller.SubscriptionController;
import com.subscriptionmanager.subscription.dto.SubscriptionResponse;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;
import com.subscriptionmanager.subscription.support.SupportEmailDraftService;
import com.subscriptionmanager.subscription.service.SubscriptionService;
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

class SubscriptionContractTest {

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
    void subscriptionsListContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(subscriptionService.list(1L)).thenReturn(List.of(sampleSubscription()));

        mockMvc.perform(get("/api/v1/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].serviceName").value("Netflix"))
                .andExpect(jsonPath("$[0].amount").value(9.99))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].billingPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$[0].nextBillingDate").exists())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void subscriptionByIdContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(subscriptionService.getById(1L, 11L)).thenReturn(sampleSubscription());

        mockMvc.perform(get("/api/v1/subscriptions/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.serviceName").value("Netflix"))
                .andExpect(jsonPath("$.amount").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.billingPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$.nextBillingDate").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void subscriptionUpcomingContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(subscriptionService.upcoming(1L, 7)).thenReturn(List.of(sampleUpcomingSubscription()));

        mockMvc.perform(get("/api/v1/subscriptions/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].serviceName").value("Netflix"))
                .andExpect(jsonPath("$[0].amount").value(9.99))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].billingPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$[0].nextBillingDate").exists())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].daysUntilBilling").value(5));

        verify(subscriptionService).upcoming(1L, 7);
    }

    private SubscriptionResponse sampleSubscription() {
        return new SubscriptionResponse(
                11L,
                "Netflix",
                "Entertainment",
                BigDecimal.valueOf(9.99),
                "USD",
                BillingPeriod.MONTHLY,
                LocalDate.of(2026, 3, 20),
                SubscriptionStatus.ACTIVE
        );
    }

    private UpcomingSubscriptionResponse sampleUpcomingSubscription() {
        return new UpcomingSubscriptionResponse(
                11L,
                "Netflix",
                "Entertainment",
                BigDecimal.valueOf(9.99),
                "USD",
                BillingPeriod.MONTHLY,
                LocalDate.of(2026, 3, 20),
                SubscriptionStatus.ACTIVE,
                5
        );
    }
}
