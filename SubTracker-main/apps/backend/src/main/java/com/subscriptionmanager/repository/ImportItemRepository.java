package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.ImportItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportItemRepository extends JpaRepository<ImportItem, Long> {
    boolean existsByJobUserIdAndExternalId(Long userId, String externalId);
    List<ImportItem> findByJobIdOrderByIdAsc(Long jobId);
}
