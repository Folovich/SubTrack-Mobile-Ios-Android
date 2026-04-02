package com.subscriptionmanager.recommendation.service;

import com.subscriptionmanager.entity.RecommendationCatalog;
import com.subscriptionmanager.recommendation.dto.RecommendationResponse;
import com.subscriptionmanager.repository.RecommendationCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationCatalogRepository recommendationCatalogRepository;

    public RecommendationServiceImpl(RecommendationCatalogRepository recommendationCatalogRepository) {
        this.recommendationCatalogRepository = recommendationCatalogRepository;
    }

    @Override
    public List<RecommendationResponse> byCategory(Long userId, String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }

        return recommendationCatalogRepository.findByCategoryNameIgnoreCase(category.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RecommendationResponse toResponse(RecommendationCatalog catalog) {
        return new RecommendationResponse(
                catalog.getCategory().getName(),
                catalog.getServiceName(),
                catalog.getAlternativeService(),
                catalog.getReason()
        );
    }
}
