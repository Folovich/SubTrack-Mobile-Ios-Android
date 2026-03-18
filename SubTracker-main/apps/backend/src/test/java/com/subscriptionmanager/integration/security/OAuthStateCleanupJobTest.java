package com.subscriptionmanager.integration.security;

import com.subscriptionmanager.repository.OAuthStateUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthStateCleanupJobTest {

    @Mock
    private OAuthStateUsageRepository oauthStateUsageRepository;

    private OAuthStateCleanupProperties properties;
    private Clock fixedClock;
    private OAuthStateCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        properties = new OAuthStateCleanupProperties();
        fixedClock = Clock.fixed(Instant.parse("2026-03-14T08:00:00Z"), ZoneOffset.UTC);
        cleanupJob = new OAuthStateCleanupJob(oauthStateUsageRepository, properties, fixedClock);
    }

    @Test
    void cleanupExpiredStatesDeletesRowsOlderThanRetentionWindow() {
        properties.setEnabled(true);
        properties.setRetentionDays(30);
        when(oauthStateUsageRepository.deleteByExpiresAtBefore(OffsetDateTime.parse("2026-02-12T08:00:00Z")))
                .thenReturn(5L);

        cleanupJob.cleanupExpiredStates();

        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(oauthStateUsageRepository).deleteByExpiresAtBefore(cutoffCaptor.capture());
        assertEquals(OffsetDateTime.parse("2026-02-12T08:00:00Z"), cutoffCaptor.getValue());
    }

    @Test
    void cleanupExpiredStatesSkipsWhenDisabled() {
        properties.setEnabled(false);
        properties.setRetentionDays(30);

        cleanupJob.cleanupExpiredStates();

        verifyNoInteractions(oauthStateUsageRepository);
    }

    @Test
    void cleanupExpiredStatesTreatsNegativeRetentionAsZero() {
        properties.setEnabled(true);
        properties.setRetentionDays(-5);
        when(oauthStateUsageRepository.deleteByExpiresAtBefore(OffsetDateTime.parse("2026-03-14T08:00:00Z")))
                .thenReturn(0L);

        cleanupJob.cleanupExpiredStates();

        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(oauthStateUsageRepository).deleteByExpiresAtBefore(cutoffCaptor.capture());
        assertEquals(OffsetDateTime.parse("2026-03-14T08:00:00Z"), cutoffCaptor.getValue());
    }
}
