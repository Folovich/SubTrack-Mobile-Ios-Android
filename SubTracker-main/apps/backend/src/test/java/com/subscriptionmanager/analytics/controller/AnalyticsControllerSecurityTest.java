package com.subscriptionmanager.analytics.controller;

import com.subscriptionmanager.analytics.service.AnalyticsService;
import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.security.CustomUserDetailsService;
import com.subscriptionmanager.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
class AnalyticsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void usageWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/usage").queryParam("period", "month"))
                .andExpect(status().isUnauthorized());
    }
}
