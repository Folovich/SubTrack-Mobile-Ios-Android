package com.subscriptionmanager.consent.service;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.consent.dto.ImportConsentStatusResponse;
import com.subscriptionmanager.entity.Consent;
import com.subscriptionmanager.entity.IntegrationConnection;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.importing.provider.ImportProviderAvailabilityPolicy;
import com.subscriptionmanager.repository.ConsentRepository;
import com.subscriptionmanager.repository.IntegrationConnectionRepository;
import com.subscriptionmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ImportConsentServiceImpl implements ImportConsentService {
    private static final String CONSENT_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    private static final String CONSENT_STATUS_GRANTED = "GRANTED";
    private static final String CONSENT_STATUS_REVOKED = "REVOKED";
    private static final String CONSENT_STATUS_NOT_GRANTED = "NOT_GRANTED";
    private static final String INTEGRATION_STATUS_NOT_CONNECTED = "NOT_CONNECTED";
    private static final String INTEGRATION_STATUS_REVOKED = "REVOKED";

    private final ConsentRepository consentRepository;
    private final IntegrationConnectionRepository integrationConnectionRepository;
    private final UserRepository userRepository;
    private final ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy;

    public ImportConsentServiceImpl(
            ConsentRepository consentRepository,
            IntegrationConnectionRepository integrationConnectionRepository,
            UserRepository userRepository,
            ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy
    ) {
        this.consentRepository = consentRepository;
        this.integrationConnectionRepository = integrationConnectionRepository;
        this.userRepository = userRepository;
        this.importProviderAvailabilityPolicy = importProviderAvailabilityPolicy;
    }

    @Override
    @Transactional
    public ImportConsentStatusResponse grant(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        if (!consentRepository.existsByUserIdAndProviderAndRevokedAtIsNull(userId, provider)) {
            Consent consent = new Consent();
            consent.setUser(user);
            consent.setProvider(provider);
            consent.setScope(CONSENT_SCOPE);
            consent.setGrantedAt(OffsetDateTime.now());
            consent.setRevokedAt(null);
            consentRepository.save(consent);
        }

        upsertIntegrationStatus(user, provider, null);
        return status(userId, provider.name());
    }

    @Override
    @Transactional
    public ImportConsentStatusResponse revoke(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        List<Consent> activeConsents = consentRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, provider);
        if (!activeConsents.isEmpty()) {
            OffsetDateTime revokedAt = OffsetDateTime.now();
            for (Consent activeConsent : activeConsents) {
                activeConsent.setRevokedAt(revokedAt);
            }
            consentRepository.saveAll(activeConsents);
        }

        userRepository.findById(userId).ifPresent(user -> {
            IntegrationConnection connection = integrationConnectionRepository
                    .findTopByUserIdAndProviderOrderByUpdatedAtDesc(user.getId(), provider)
                    .orElseGet(() -> {
                        IntegrationConnection created = new IntegrationConnection();
                        created.setUser(user);
                        created.setProvider(provider);
                        created.setCreatedAt(OffsetDateTime.now());
                        return created;
                    });

            connection.setStatus(INTEGRATION_STATUS_REVOKED);
            connection.setEncryptedAccessToken(null);
            connection.setEncryptedRefreshToken(null);
            connection.setTokenExpiresAt(null);
            connection.setUpdatedAt(OffsetDateTime.now());
            integrationConnectionRepository.save(connection);
        });
        return status(userId, provider.name());
    }

    @Override
    @Transactional(readOnly = true)
    public ImportConsentStatusResponse status(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        Optional<Consent> latestConsent = consentRepository.findTopByUserIdAndProviderOrderByGrantedAtDesc(userId, provider);
        String integrationStatus = integrationConnectionRepository
                .findTopByUserIdAndProviderOrderByUpdatedAtDesc(userId, provider)
                .map(IntegrationConnection::getStatus)
                .orElse(INTEGRATION_STATUS_NOT_CONNECTED);

        if (latestConsent.isEmpty()) {
            return new ImportConsentStatusResponse(
                    provider.name(),
                    CONSENT_STATUS_NOT_GRANTED,
                    CONSENT_SCOPE,
                    null,
                    null,
                    integrationStatus
            );
        }

        Consent consent = latestConsent.get();
        String status = consent.getRevokedAt() == null ? CONSENT_STATUS_GRANTED : CONSENT_STATUS_REVOKED;
        return new ImportConsentStatusResponse(
                provider.name(),
                status,
                consent.getScope(),
                consent.getGrantedAt(),
                consent.getRevokedAt(),
                integrationStatus
        );
    }

    private void upsertIntegrationStatus(User user, ImportProvider provider, String status) {
        IntegrationConnection connection = integrationConnectionRepository
                .findTopByUserIdAndProviderOrderByUpdatedAtDesc(user.getId(), provider)
                .orElseGet(() -> {
                    IntegrationConnection created = new IntegrationConnection();
                    created.setUser(user);
                    created.setProvider(provider);
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });

        if (status != null) {
            connection.setStatus(status);
        } else if (connection.getStatus() == null || connection.getStatus().isBlank()) {
            connection.setStatus(INTEGRATION_STATUS_NOT_CONNECTED);
        }
        connection.setUpdatedAt(OffsetDateTime.now());
        integrationConnectionRepository.save(connection);
    }
}
