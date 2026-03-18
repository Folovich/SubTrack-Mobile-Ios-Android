package com.subscriptionmanager.recommendation.dto;

public record RecommendationResponse(String category, String currentService, String alternativeService, String reason) {
}
