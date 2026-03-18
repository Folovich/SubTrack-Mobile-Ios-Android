package com.subscriptionmanager.integration.security;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.entity.OAuthStateUsage;
import com.subscriptionmanager.repository.OAuthStateUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthStateTokenServiceTest {

    @Mock
    private OAuthStateUsageRepository oauthStateUsageRepository;

    private OAuthStateTokenService oauthStateTokenService;

    @BeforeEach
    void setUp() {
        IntegrationKeyResolver keyResolver = new IntegrationKeyResolver(
                "subtrack-dev-secret-change-me-at-least-32-bytes",
                "subtrack-dev-secret-change-me-at-least-32-bytes"
        );
        oauthStateTokenService = new OAuthStateTokenService(keyResolver, oauthStateUsageRepository);
    }

    @Test
    void parseAndConsumeAllowsValidStateOnFirstUse() {
        when(oauthStateUsageRepository.saveAndFlush(any(OAuthStateUsage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        String state = oauthStateTokenService.issue(10L, ImportProvider.GMAIL, 10);

        OAuthStateTokenService.StatePayload payload =
                oauthStateTokenService.parseAndConsume(state, ImportProvider.GMAIL);

        assertEquals(10L, payload.userId());
        assertEquals(ImportProvider.GMAIL, payload.provider());

        ArgumentCaptor<OAuthStateUsage> usageCaptor = ArgumentCaptor.forClass(OAuthStateUsage.class);
        verify(oauthStateUsageRepository).saveAndFlush(usageCaptor.capture());
        OAuthStateUsage persisted = usageCaptor.getValue();
        assertEquals(10L, persisted.getUserId());
        assertEquals(ImportProvider.GMAIL, persisted.getProvider());
        assertFalse(persisted.getStateJti().isBlank());
    }

    @Test
    void parseAndConsumeRejectsStateReplay() {
        String state = oauthStateTokenService.issue(10L, ImportProvider.GMAIL, 10);
        when(oauthStateUsageRepository.saveAndFlush(any(OAuthStateUsage.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate state"));
        when(oauthStateUsageRepository.existsById(anyString())).thenReturn(true);

        OAuthStateTokenService.StateValidationException exception = assertThrows(
                OAuthStateTokenService.StateValidationException.class,
                () -> oauthStateTokenService.parseAndConsume(state, ImportProvider.GMAIL)
        );

        assertEquals(OAuthStateTokenService.StateErrorReason.STATE_REPLAY, exception.reason());
    }

    @Test
    void parseAndConsumeRejectsExpiredState() {
        String expiredState = oauthStateTokenService.issue(10L, ImportProvider.GMAIL, -1);

        OAuthStateTokenService.StateValidationException exception = assertThrows(
                OAuthStateTokenService.StateValidationException.class,
                () -> oauthStateTokenService.parseAndConsume(expiredState, ImportProvider.GMAIL)
        );

        assertEquals(OAuthStateTokenService.StateErrorReason.EXPIRED_STATE, exception.reason());
        verify(oauthStateUsageRepository, never()).saveAndFlush(any(OAuthStateUsage.class));
    }
}
