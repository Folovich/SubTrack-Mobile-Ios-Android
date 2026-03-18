package com.subscriptionmanager.repository;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository extends JpaRepository<Consent, Long> {
    List<Consent> findByUserId(Long userId);
    boolean existsByUserIdAndProviderAndRevokedAtIsNull(Long userId, ImportProvider provider);
    List<Consent> findByUserIdAndProviderAndRevokedAtIsNull(Long userId, ImportProvider provider);
    Optional<Consent> findTopByUserIdAndProviderOrderByGrantedAtDesc(Long userId, ImportProvider provider);
}
