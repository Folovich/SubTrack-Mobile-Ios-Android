package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.OAuthStateUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface OAuthStateUsageRepository extends JpaRepository<OAuthStateUsage, String> {
    long deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
