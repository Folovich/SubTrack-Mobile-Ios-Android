package com.subscriptionmanager.recommendation.controller;

import com.subscriptionmanager.recommendation.dto.RecommendationResponse;
import com.subscriptionmanager.recommendation.service.RecommendationService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    public RecommendationController(RecommendationService recommendationService, CurrentUserService currentUserService) {
        this.recommendationService = recommendationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<RecommendationResponse> list(@RequestParam String category) {
        Long userId = currentUserService.requireCurrentUserId();
        return recommendationService.byCategory(userId, category);
    }
}
