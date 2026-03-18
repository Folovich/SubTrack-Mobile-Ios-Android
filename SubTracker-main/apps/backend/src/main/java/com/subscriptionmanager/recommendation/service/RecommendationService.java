package com.subscriptionmanager.recommendation.service;

import com.subscriptionmanager.recommendation.dto.RecommendationResponse;

import java.util.List;

public interface RecommendationService {
    List<RecommendationResponse> byCategory(Long userId, String category);
}
