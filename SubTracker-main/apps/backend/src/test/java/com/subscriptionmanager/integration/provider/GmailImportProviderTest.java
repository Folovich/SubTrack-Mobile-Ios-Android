package com.subscriptionmanager.integration.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.consent.service.ImportConsentService;
import com.subscriptionmanager.entity.IntegrationConnection;
import com.subscriptionmanager.entity.OAuthStateUsage;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.importing.config.ImportProviderFeatureFlagsProperties;
import com.subscriptionmanager.importing.exception.MailboxReauthRequiredException;
import com.subscriptionmanager.importing.provider.ImportProviderAvailabilityPolicy;
import com.subscriptionmanager.importing.provider.ImportProviderScaffoldRegistry;
import com.subscriptionmanager.integration.config.GmailIntegrationProperties;
import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;
import com.subscriptionmanager.integration.security.IntegrationKeyResolver;
import com.subscriptionmanager.integration.security.OAuthStateTokenService;
import com.subscriptionmanager.integration.security.TokenEncryptionService;
import com.subscriptionmanager.repository.IntegrationConnectionRepository;
import com.subscriptionmanager.repository.OAuthStateUsageRepository;
import com.subscriptionmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailImportProviderTest {
    @Mock
    private IntegrationConnectionRepository integrationConnectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImportConsentService importConsentService;
    @Mock
    private GoogleGmailApiClient googleGmailApiClient;
    @Mock
    private OAuthStateUsageRepository oauthStateUsageRepository;

    private GmailImportProvider provider;
    private TokenEncryptionService tokenEncryptionService;
    private OAuthStateTokenService oauthStateTokenService;

    @BeforeEach
    void setUp() {
        ImportProviderFeatureFlagsProperties providerFlags = new ImportProviderFeatureFlagsProperties();
        providerFlags.getGmail().setEnabled(true);
        providerFlags.getGmail().setMailboxEnabled(true);

        ImportProviderAvailabilityPolicy availabilityPolicy = new ImportProviderAvailabilityPolicy(
                providerFlags,
                new ImportProviderScaffoldRegistry()
        );

        GmailIntegrationProperties properties = new GmailIntegrationProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri("http://localhost:8080/api/v1/integrations/GMAIL/oauth/callback");
        properties.setFrontendRedirectUri("http://localhost:5173/import");
        properties.setMaxResults(20);
        properties.setInitialLookbackDays(30);

        IntegrationKeyResolver keyResolver = new IntegrationKeyResolver(
                "subtrack-dev-secret-change-me-at-least-32-bytes",
                "subtrack-dev-secret-change-me-at-least-32-bytes"
        );
        tokenEncryptionService = new TokenEncryptionService(keyResolver);
        oauthStateTokenService = new OAuthStateTokenService(keyResolver, oauthStateUsageRepository);

        provider = new GmailImportProvider(
                availabilityPolicy,
                integrationConnectionRepository,
                userRepository,
                importConsentService,
                tokenEncryptionService,
                oauthStateTokenService,
                googleGmailApiClient,
                properties
        );
    }

    @Test
    void handleAuthorizationCallbackStoresConnectedGmailTokens() {
        User user = user(1L);
        String state = oauthStateTokenService.issue(1L, ImportProvider.GMAIL, 10);
        when(oauthStateUsageRepository.saveAndFlush(any(OAuthStateUsage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(googleGmailApiClient.exchangeCode("auth-code"))
                .thenReturn(new GoogleGmailApiClient.TokenResponse("access-123", "refresh-123", 3600));
        when(googleGmailApiClient.getProfile("access-123"))
                .thenReturn(new GoogleGmailApiClient.GmailProfile("person@gmail.com", "history-1"));
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.empty());
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String redirect = provider.handleAuthorizationCallback("auth-code", state, null);

        ArgumentCaptor<IntegrationConnection> captor = ArgumentCaptor.forClass(IntegrationConnection.class);
        verify(integrationConnectionRepository).save(captor.capture());

        IntegrationConnection saved = captor.getValue();
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("person@gmail.com", saved.getExternalAccountEmail());
        assertEquals("access-123", tokenEncryptionService.decrypt(saved.getEncryptedAccessToken()));
        assertEquals("refresh-123", tokenEncryptionService.decrypt(saved.getEncryptedRefreshToken()));
        assertEquals("http://localhost:5173/import?gmail=connected", redirect);
    }

    @Test
    void fetchMailboxMessagesRefreshesExpiredTokenAndMapsGmailMessage() throws Exception {
        User user = user(1L);
        IntegrationConnection connection = new IntegrationConnection();
        connection.setUser(user);
        connection.setProvider(ImportProvider.GMAIL);
        connection.setStatus("ACTIVE");
        connection.setEncryptedRefreshToken(tokenEncryptionService.encrypt("refresh-123"));
        connection.setTokenExpiresAt(OffsetDateTime.now().minusMinutes(5));

        String body = "Your Netflix renewal is scheduled for 2026-03-20. Amount: 9.99 USD";
        String encodedBody = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(body.getBytes());
        ObjectMapper objectMapper = new ObjectMapper();

        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.of(connection));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(googleGmailApiClient.refreshAccessToken("refresh-123"))
                .thenReturn(new GoogleGmailApiClient.TokenResponse("new-access", null, 3600));
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        when(googleGmailApiClient.listMessages(eq("new-access"), queryCaptor.capture(), eq(20)))
                .thenReturn(List.of(new GoogleGmailApiClient.GmailMessageSummary("gmail-msg-1", "thread-1")));
        when(googleGmailApiClient.getMessage("new-access", "gmail-msg-1"))
                .thenReturn(new GoogleGmailApiClient.GmailMessage(
                        "gmail-msg-1",
                        "thread-1",
                        "Netflix renewal",
                        "history-1",
                        String.valueOf(Instant.parse("2026-03-12T10:00:00Z").toEpochMilli()),
                        Map.of(
                                "From", "Netflix <billing@netflix.com>",
                                "Subject", "Netflix renewal"
                        ),
                        objectMapper.readTree("""
                                {
                                  "mimeType": "multipart/alternative",
                                  "parts": [
                                    {
                                      "mimeType": "text/plain",
                                      "body": {
                                        "data": "%s"
                                      }
                                    }
                                  ]
                                }
                                """.formatted(encodedBody))
                ));

        MailImportProvider.MailboxFetchResult fetchResult = provider.fetchMailboxMessages(1L);

        assertEquals(1, fetchResult.messages().size());
        assertEquals("gmail-msg-1", fetchResult.messages().getFirst().externalId());
        assertEquals("billing@netflix.com", fetchResult.messages().getFirst().from());
        assertEquals(body, fetchResult.messages().getFirst().body());
        assertEquals("new-access", tokenEncryptionService.decrypt(connection.getEncryptedAccessToken()));
        String query = queryCaptor.getValue();
        assertTrue(query.contains("in:inbox"));
        assertTrue(query.contains("after:"));
        assertTrue(query.contains(" OR "));
        for (String keyword : List.of("billing", "renewal", "payment", "invoice", "receipt", "subscription", "charged", "plan")) {
            assertTrue(query.contains(keyword), "Query must contain keyword: " + keyword);
        }
    }

    @Test
    void fetchMailboxMessagesMarksConnectionAsReauthRequiredWhenRefreshTokenIsRevoked() {
        User user = user(1L);
        IntegrationConnection connection = new IntegrationConnection();
        connection.setUser(user);
        connection.setProvider(ImportProvider.GMAIL);
        connection.setStatus("ACTIVE");
        connection.setEncryptedRefreshToken(tokenEncryptionService.encrypt("refresh-123"));
        connection.setTokenExpiresAt(OffsetDateTime.now().minusMinutes(5));

        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.of(connection));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(googleGmailApiClient.refreshAccessToken("refresh-123"))
                .thenThrow(new GmailApiClientException(400, "invalid_grant", "Token has been revoked"));

        MailboxReauthRequiredException exception = assertThrows(
                MailboxReauthRequiredException.class,
                () -> provider.fetchMailboxMessages(1L)
        );

        assertEquals("gmail access expired or was revoked for provider GMAIL. Reconnect Gmail and retry.", exception.getMessage());
        assertEquals("REAUTH_REQUIRED", connection.getStatus());
        assertEquals("TOKEN_REVOKED", connection.getLastErrorCode());
        assertNull(connection.getEncryptedAccessToken());
        assertNull(connection.getEncryptedRefreshToken());
    }

    @Test
    void disconnectRevokesTokensAndConsent() {
        User user = user(1L);
        IntegrationConnection connection = new IntegrationConnection();
        connection.setUser(user);
        connection.setProvider(ImportProvider.GMAIL);
        connection.setStatus("ACTIVE");
        connection.setEncryptedAccessToken(tokenEncryptionService.encrypt("access-123"));
        connection.setEncryptedRefreshToken(tokenEncryptionService.encrypt("refresh-123"));
        connection.setCreatedAt(OffsetDateTime.ofInstant(Instant.parse("2026-03-10T09:00:00Z"), ZoneOffset.UTC));
        connection.setUpdatedAt(connection.getCreatedAt());

        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.of(connection));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IntegrationConnectionResponse response = provider.disconnect(1L);

        verify(googleGmailApiClient).revokeToken("access-123");
        verify(importConsentService).revoke(1L, "GMAIL");
        assertEquals("REVOKED", response.status());
        assertNull(connection.getEncryptedAccessToken());
        assertNull(connection.getEncryptedRefreshToken());
    }

    @Test
    void handleAuthorizationCallbackReturnsReplayReasonWhenStateIsReused() {
        String state = oauthStateTokenService.issue(1L, ImportProvider.GMAIL, 10);
        when(oauthStateUsageRepository.saveAndFlush(any(OAuthStateUsage.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate state"));
        when(oauthStateUsageRepository.existsById(any())).thenReturn(true);

        String redirect = provider.handleAuthorizationCallback("auth-code", state, null);

        assertEquals("http://localhost:5173/import?gmail=error&reason=state_replay", redirect);
    }

    @Test
    void handleAuthorizationCallbackReturnsExpiredReasonForExpiredState() {
        String expiredState = oauthStateTokenService.issue(1L, ImportProvider.GMAIL, -1);

        String redirect = provider.handleAuthorizationCallback("auth-code", expiredState, null);

        assertEquals("http://localhost:5173/import?gmail=error&reason=expired_state", redirect);
    }

    @Test
    void handleAuthorizationCallbackReturnsProviderMismatchReasonForForeignProviderState() {
        String yandexState = oauthStateTokenService.issue(1L, ImportProvider.YANDEX, 10);

        String redirect = provider.handleAuthorizationCallback("auth-code", yandexState, null);

        assertEquals("http://localhost:5173/import?gmail=error&reason=provider_mismatch", redirect);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        return user;
    }
}
