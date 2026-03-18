package com.subscriptionmanager.usage.controller;

import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.security.CustomUserDetailsService;
import com.subscriptionmanager.security.JwtAuthenticationFilter;
import com.subscriptionmanager.usage.service.UsageSignalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsageSignalController.class)
class UsageSignalControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsageSignalService usageSignalService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void usageSignalsWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/usage-signals"))
                .andExpect(status().isUnauthorized());
    }
}
