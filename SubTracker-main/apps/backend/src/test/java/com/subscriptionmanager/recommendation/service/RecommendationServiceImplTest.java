package com.subscriptionmanager.recommendation.service;

import com.subscriptionmanager.entity.Category;
import com.subscriptionmanager.entity.RecommendationCatalog;
import com.subscriptionmanager.recommendation.dto.RecommendationResponse;
import com.subscriptionmanager.repository.RecommendationCatalogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RecommendationCatalogRepository recommendationCatalogRepository;

    @Test
    void byCategoryReturnsNonEmptyListForSeededCategory() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(recommendationCatalogRepository);

        Category category = new Category();
        category.setName("Entertainment");

        RecommendationCatalog catalog = new RecommendationCatalog();
        catalog.setCategory(category);
        catalog.setServiceName("Netflix");
        catalog.setAlternativeService("Amazon Prime Video");
        catalog.setReason("Lower annual cost and bundled shipping benefits");

        when(recommendationCatalogRepository.findByCategoryNameIgnoreCase("Entertainment"))
                .thenReturn(List.of(catalog));

        List<RecommendationResponse> result = service.byCategory(1L, "Entertainment");

        assertEquals(1, result.size());
        assertEquals("Entertainment", result.getFirst().category());
        assertEquals("Netflix", result.getFirst().currentService());
        assertEquals("Amazon Prime Video", result.getFirst().alternativeService());
        assertEquals("Lower annual cost and bundled shipping benefits", result.getFirst().reason());
    }

    @Test
    void byCategoryReturnsEmptyListForUnknownCategory() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(recommendationCatalogRepository);

        when(recommendationCatalogRepository.findByCategoryNameIgnoreCase("Unknown"))
                .thenReturn(List.of());

        List<RecommendationResponse> result = service.byCategory(1L, "Unknown");

        assertEquals(0, result.size());
    }
}
