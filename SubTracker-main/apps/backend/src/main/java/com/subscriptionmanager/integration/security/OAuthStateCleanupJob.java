package com.subscriptionmanager.integration.security;

import com.subscriptionmanager.repository.OAuthStateUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class OAuthStateCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(OAuthStateCleanupJob.class);

    private final OAuthStateUsageRepository oauthStateUsageRepository;
    private final OAuthStateCleanupProperties properties;
    private final Clock clock;

    public OAuthStateCleanupJob(
            OAuthStateUsageRepository oauthStateUsageRepository,
            OAuthStateCleanupProperties properties,
            Clock clock
    ) {
        this.oauthStateUsageRepository = oauthStateUsageRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.integrations.security.oauth-state-cleanup.cron:0 30 * * * *}")
    @Transactional
    public void cleanupExpiredStates() {
        if (!properties.isEnabled()) {
            return;
        }

        int retentionDays = Math.max(0, properties.getRetentionDays());
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(retentionDays);
        long deletedRows = oauthStateUsageRepository.deleteByExpiresAtBefore(cutoff);
        if (deletedRows > 0) {
            log.info("OAuth state cleanup removed {} expired rows (retentionDays={})", deletedRows, retentionDays);
        }
    }
}
