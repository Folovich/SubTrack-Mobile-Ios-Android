package com.subscriptionmanager.recommendation.controller;

import com.subscriptionmanager.recommendation.service.RecommendationService;
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

@WebMvcTest(controllers = RecommendationController.class)
class RecommendationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void recommendationsWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations").queryParam("category", "Entertainment"))
                .andExpect(status().isUnauthorized());
    }
}
