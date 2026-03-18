package com.subscriptionmanager.recommendation.controller;

import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.recommendation.dto.RecommendationResponse;
import com.subscriptionmanager.recommendation.service.RecommendationService;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationControllerTest {

    private RecommendationService recommendationService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        recommendationService = Mockito.mock(RecommendationService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        RecommendationController controller = new RecommendationController(recommendationService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsRecommendationShapeForCategory() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(recommendationService.byCategory(1L, "Entertainment"))
                .thenReturn(List.of(new RecommendationResponse(
                        "Entertainment",
                        "Netflix",
                        "Amazon Prime Video",
                        "Lower annual cost and bundled shipping benefits"
                )));

        mockMvc.perform(get("/api/v1/recommendations").queryParam("category", "Entertainment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Entertainment"))
                .andExpect(jsonPath("$[0].currentService").value("Netflix"))
                .andExpect(jsonPath("$[0].alternativeService").value("Amazon Prime Video"))
                .andExpect(jsonPath("$[0].reason").value("Lower annual cost and bundled shipping benefits"));

        verify(recommendationService).byCategory(1L, "Entertainment");
    }
}
