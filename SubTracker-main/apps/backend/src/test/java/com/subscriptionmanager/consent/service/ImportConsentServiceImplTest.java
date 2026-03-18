package com.subscriptionmanager.consent.service;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.consent.dto.ImportConsentStatusResponse;
import com.subscriptionmanager.entity.Consent;
import com.subscriptionmanager.entity.IntegrationConnection;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.importing.config.ImportProviderFeatureFlagsProperties;
import com.subscriptionmanager.importing.provider.ImportProviderAvailabilityPolicy;
import com.subscriptionmanager.importing.provider.ImportProviderScaffoldRegistry;
import com.subscriptionmanager.repository.ConsentRepository;
import com.subscriptionmanager.repository.IntegrationConnectionRepository;
import com.subscriptionmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportConsentServiceImplTest {

    @Mock
    private ConsentRepository consentRepository;
    @Mock
    private IntegrationConnectionRepository integrationConnectionRepository;
    @Mock
    private UserRepository userRepository;

    private ImportConsentServiceImpl service;
    private ImportProviderFeatureFlagsProperties providerFlags;

    @BeforeEach
    void setUp() {
        providerFlags = defaultProviderFlags();
        service = new ImportConsentServiceImpl(
                consentRepository,
                integrationConnectionRepository,
                userRepository,
                new ImportProviderAvailabilityPolicy(providerFlags, new ImportProviderScaffoldRegistry())
        );
    }

    @Test
    void grantCreatesConsentWithoutMarkingMailboxAsConnected() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(consentRepository.existsByUserIdAndProviderAndRevokedAtIsNull(1L, ImportProvider.GMAIL))
                .thenReturn(false);
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(1L, ImportProvider.GMAIL))
                .thenAnswer(invocation -> {
                    Consent consent = new Consent();
                    consent.setProvider(ImportProvider.GMAIL);
                    consent.setScope("https://www.googleapis.com/auth/gmail.readonly");
                    consent.setGrantedAt(OffsetDateTime.now());
                    consent.setRevokedAt(null);
                    return Optional.of(consent);
                });
        IntegrationConnection activeIntegration = existingIntegration(user(1L), ImportProvider.GMAIL);
        activeIntegration.setStatus("NOT_CONNECTED");
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.empty(), Optional.of(activeIntegration));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportConsentStatusResponse response = service.grant(1L, "gmail");

        assertEquals("GMAIL", response.provider());
        assertEquals("GRANTED", response.status());
        assertEquals("https://www.googleapis.com/auth/gmail.readonly", response.scope());
        assertEquals("NOT_CONNECTED", response.integrationStatus());
    }

    @Test
    void revokeReturnsRevokedStatus() {
        Consent active = new Consent();
        active.setProvider(ImportProvider.GMAIL);
        active.setScope("https://www.googleapis.com/auth/gmail.readonly");
        active.setGrantedAt(OffsetDateTime.parse("2026-03-09T09:00:00Z"));
        active.setRevokedAt(null);

        when(consentRepository.findByUserIdAndProviderAndRevokedAtIsNull(1L, ImportProvider.GMAIL))
                .thenReturn(List.of(active));
        when(consentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.of(existingIntegration(user(1L), ImportProvider.GMAIL)));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(1L, ImportProvider.GMAIL))
                .thenAnswer(invocation -> Optional.of(active));

        ImportConsentStatusResponse response = service.revoke(1L, "GMAIL");

        assertEquals("REVOKED", response.status());
        assertEquals("REVOKED", response.integrationStatus());
    }

    @Test
    void statusReturnsNotGrantedWhenConsentMissing() {
        when(consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.empty());
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.GMAIL))
                .thenReturn(Optional.empty());

        ImportConsentStatusResponse response = service.status(1L, "GMAIL");

        assertEquals("NOT_GRANTED", response.status());
        assertEquals("NOT_CONNECTED", response.integrationStatus());
    }

    @Test
    void unsupportedProviderKeepsLegacyMessageWhenFlagsAreOff() {
        ApiException exception = assertThrows(ApiException.class, () -> service.status(1L, "YANDEX"));
        assertEquals("provider must be one of: GMAIL", exception.getMessage());
    }

    @Test
    void grantKeepsLegacyMessageForNonGmailWhenFlagsAreOff() {
        ApiException exception = assertThrows(ApiException.class, () -> service.grant(1L, "YANDEX"));
        assertEquals("provider must be one of: GMAIL", exception.getMessage());
    }

    @Test
    void revokeKeepsLegacyMessageForNonGmailWhenFlagsAreOff() {
        ApiException exception = assertThrows(ApiException.class, () -> service.revoke(1L, "YANDEX"));
        assertEquals("provider must be one of: GMAIL", exception.getMessage());
    }

    @Test
    void grantEnabledProviderByFlagUsesPersistenceModelWithoutValidationBlock() {
        providerFlags.getYandex().setEnabled(true);
        service = new ImportConsentServiceImpl(
                consentRepository,
                integrationConnectionRepository,
                userRepository,
                new ImportProviderAvailabilityPolicy(providerFlags, new ImportProviderScaffoldRegistry())
        );

        User user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(consentRepository.existsByUserIdAndProviderAndRevokedAtIsNull(1L, ImportProvider.YANDEX))
                .thenReturn(false);
        when(consentRepository.save(any(Consent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(1L, ImportProvider.YANDEX))
                .thenAnswer(invocation -> {
                    Consent consent = new Consent();
                    consent.setProvider(ImportProvider.YANDEX);
                    consent.setScope("https://www.googleapis.com/auth/gmail.readonly");
                    consent.setGrantedAt(OffsetDateTime.now());
                    consent.setRevokedAt(null);
                    return Optional.of(consent);
                });
        IntegrationConnection activeIntegration = existingIntegration(user, ImportProvider.YANDEX);
        activeIntegration.setStatus("NOT_CONNECTED");
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.YANDEX))
                .thenReturn(Optional.empty(), Optional.of(activeIntegration));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportConsentStatusResponse response = service.grant(1L, "YANDEX");

        assertEquals("YANDEX", response.provider());
        assertEquals("GRANTED", response.status());
        assertEquals("NOT_CONNECTED", response.integrationStatus());
    }

    @Test
    void statusEnabledProviderByFlagReturnsNotGrantedWithoutRuntimeError() {
        providerFlags.getYandex().setEnabled(true);
        service = new ImportConsentServiceImpl(
                consentRepository,
                integrationConnectionRepository,
                userRepository,
                new ImportProviderAvailabilityPolicy(providerFlags, new ImportProviderScaffoldRegistry())
        );

        when(consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(1L, ImportProvider.YANDEX))
                .thenReturn(Optional.empty());
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.YANDEX))
                .thenReturn(Optional.empty());

        ImportConsentStatusResponse response = service.status(1L, "YANDEX");

        assertEquals("YANDEX", response.provider());
        assertEquals("NOT_GRANTED", response.status());
        assertEquals("NOT_CONNECTED", response.integrationStatus());
    }

    @Test
    void revokeEnabledProviderByFlagUsesPersistenceModelWithoutRuntimeError() {
        providerFlags.getYandex().setEnabled(true);
        service = new ImportConsentServiceImpl(
                consentRepository,
                integrationConnectionRepository,
                userRepository,
                new ImportProviderAvailabilityPolicy(providerFlags, new ImportProviderScaffoldRegistry())
        );

        Consent active = new Consent();
        active.setProvider(ImportProvider.YANDEX);
        active.setScope("https://www.googleapis.com/auth/gmail.readonly");
        active.setGrantedAt(OffsetDateTime.parse("2026-03-09T09:00:00Z"));
        active.setRevokedAt(null);

        when(consentRepository.findByUserIdAndProviderAndRevokedAtIsNull(1L, ImportProvider.YANDEX))
                .thenReturn(List.of(active));
        when(consentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(1L, ImportProvider.YANDEX))
                .thenReturn(Optional.of(existingIntegration(user, ImportProvider.YANDEX)));
        when(integrationConnectionRepository.save(any(IntegrationConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(1L, ImportProvider.YANDEX))
                .thenReturn(Optional.of(active));

        ImportConsentStatusResponse response = service.revoke(1L, "YANDEX");

        assertEquals("YANDEX", response.provider());
        assertEquals("REVOKED", response.status());
        assertEquals("REVOKED", response.integrationStatus());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private IntegrationConnection existingIntegration(User user, ImportProvider provider) {
        IntegrationConnection connection = new IntegrationConnection();
        connection.setUser(user);
        connection.setProvider(provider);
        connection.setStatus("ACTIVE");
        connection.setCreatedAt(OffsetDateTime.now());
        connection.setUpdatedAt(OffsetDateTime.now());
        return connection;
    }

    private ImportProviderFeatureFlagsProperties defaultProviderFlags() {
        ImportProviderFeatureFlagsProperties flags = new ImportProviderFeatureFlagsProperties();
        flags.getGmail().setEnabled(true);
        flags.getGmail().setMailboxEnabled(true);
        flags.getYandex().setEnabled(false);
        flags.getMailRu().setEnabled(false);
        flags.getBankApi().setEnabled(false);
        return flags;
    }
}
