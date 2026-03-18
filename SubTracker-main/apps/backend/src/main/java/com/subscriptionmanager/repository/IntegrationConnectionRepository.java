package com.subscriptionmanager.repository;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.entity.IntegrationConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {
    List<IntegrationConnection> findByUserId(Long userId);
    Optional<IntegrationConnection> findTopByUserIdAndProviderOrderByUpdatedAtDesc(Long userId, ImportProvider provider);
}
