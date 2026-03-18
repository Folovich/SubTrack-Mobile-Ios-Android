package com.subscriptionmanager.integration.service;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.importing.provider.ImportProviderAvailabilityPolicy;
import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;
import com.subscriptionmanager.integration.dto.OAuthStartResponse;
import com.subscriptionmanager.integration.provider.MailImportProvider;
import com.subscriptionmanager.integration.provider.MailImportProviderRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntegrationServiceImpl implements IntegrationService {
    private final ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy;
    private final MailImportProviderRegistry mailImportProviderRegistry;

    public IntegrationServiceImpl(
            ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy,
            MailImportProviderRegistry mailImportProviderRegistry
    ) {
        this.importProviderAvailabilityPolicy = importProviderAvailabilityPolicy;
        this.mailImportProviderRegistry = mailImportProviderRegistry;
    }

    @Override
    public List<IntegrationConnectionResponse> list(Long userId) {
        return List.of(status(userId, ImportProvider.GMAIL.name()));
    }

    @Override
    public IntegrationConnectionResponse status(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        return providerService(provider).connectionStatus(userId);
    }

    @Override
    public OAuthStartResponse startOAuth(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        importProviderAvailabilityPolicy.requireEnabledForMailboxFlow(provider);
        String authorizationUrl = providerService(provider).buildAuthorizationUrl(userId);
        return new OAuthStartResponse(provider.name(), authorizationUrl);
    }

    @Override
    public String handleOAuthCallback(String providerRaw, String code, String state, String error) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        return providerService(provider).handleAuthorizationCallback(code, state, error);
    }

    @Override
    public IntegrationConnectionResponse disconnect(Long userId, String providerRaw) {
        ImportProvider provider = importProviderAvailabilityPolicy.resolveEnabledForConsent(providerRaw);
        return providerService(provider).disconnect(userId);
    }

    private MailImportProvider providerService(ImportProvider provider) {
        return mailImportProviderRegistry.require(provider);
    }
}
