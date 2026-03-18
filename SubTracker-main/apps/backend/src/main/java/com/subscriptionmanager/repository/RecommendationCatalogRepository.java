package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.RecommendationCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface RecommendationCatalogRepository extends JpaRepository<RecommendationCatalog, Long> {
    @EntityGraph(attributePaths = "category")
    List<RecommendationCatalog> findByCategoryNameIgnoreCase(String categoryName);
}
