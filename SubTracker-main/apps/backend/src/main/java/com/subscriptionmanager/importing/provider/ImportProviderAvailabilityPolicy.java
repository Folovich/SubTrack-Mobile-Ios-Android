package com.subscriptionmanager.importing.provider;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.importing.config.ImportProviderFeatureFlagsProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ImportProviderAvailabilityPolicy {
    public static final String LEGACY_IMPORT_UNSUPPORTED_MESSAGE = "only GMAIL provider is supported in MVP";
    public static final String LEGACY_CONSENT_UNSUPPORTED_MESSAGE = "provider must be one of: GMAIL";

    private static final String PROVIDER_REQUIRED_MESSAGE = "provider is required";
    private static final String IMPORT_NOT_IMPLEMENTED_MESSAGE_TEMPLATE =
            "provider %s is enabled but import flow is not implemented yet";
    private static final String IMPORT_DISABLED_MESSAGE_TEMPLATE =
            "provider %s is disabled by feature flag";
    private static final String MAILBOX_FLOW_DISABLED_MESSAGE_TEMPLATE =
            "provider %s mailbox import flow is disabled by feature flag";

    private final ImportProviderFeatureFlagsProperties featureFlags;
    private final ImportProviderScaffoldRegistry scaffoldRegistry;

    public ImportProviderAvailabilityPolicy(
            ImportProviderFeatureFlagsProperties featureFlags,
            ImportProviderScaffoldRegistry scaffoldRegistry
    ) {
        this.featureFlags = featureFlags;
        this.scaffoldRegistry = scaffoldRegistry;
    }

    public ImportProvider requireEnabledForImport(ImportProvider provider) {
        if (provider == null) {
            throw new ApiException(PROVIDER_REQUIRED_MESSAGE);
        }

        if (!isEnabled(provider)) {
            throw new ApiException(importDisabledMessage(provider));
        }

        return provider;
    }

    public void requireImplementedForImport(ImportProvider provider) {
        if (!scaffoldRegistry.isImportFlowImplemented(provider)) {
            throw new ApiException(IMPORT_NOT_IMPLEMENTED_MESSAGE_TEMPLATE.formatted(provider.name()));
        }
    }

    public void requireEnabledForMailboxFlow(ImportProvider provider) {
        requireEnabledForImport(provider);
        requireImplementedForImport(provider);
        if (!featureFlags.isMailboxFlowEnabled(provider)) {
            throw new ApiException(MAILBOX_FLOW_DISABLED_MESSAGE_TEMPLATE.formatted(provider.name()));
        }
    }

    public ImportProvider resolveEnabledForConsent(String providerRaw) {
        if (providerRaw == null || providerRaw.isBlank()) {
            throw new ApiException(PROVIDER_REQUIRED_MESSAGE);
        }

        ImportProvider provider;
        try {
            String normalizedProvider = providerRaw.trim().toUpperCase(Locale.ROOT).replace("-", "_");
            provider = ImportProvider.valueOf(normalizedProvider);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(consentUnsupportedMessage());
        }

        if (!isEnabled(provider)) {
            throw new ApiException(consentUnsupportedMessage());
        }

        return provider;
    }

    private boolean isEnabled(ImportProvider provider) {
        return featureFlags.isEnabled(provider);
    }

    private String importDisabledMessage(ImportProvider provider) {
        if (featureFlags.isOnlyGmailEnabled() && provider != ImportProvider.GMAIL) {
            return LEGACY_IMPORT_UNSUPPORTED_MESSAGE;
        }
        return IMPORT_DISABLED_MESSAGE_TEMPLATE.formatted(provider.name());
    }

    private String consentUnsupportedMessage() {
        if (featureFlags.isOnlyGmailEnabled()) {
            return LEGACY_CONSENT_UNSUPPORTED_MESSAGE;
        }

        List<ImportProvider> enabledProviders = enabledProviders();
        if (enabledProviders.isEmpty()) {
            return LEGACY_CONSENT_UNSUPPORTED_MESSAGE;
        }

        String allowedProviders = enabledProviders.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return "provider must be one of: " + allowedProviders;
    }

    private List<ImportProvider> enabledProviders() {
        return Arrays.stream(ImportProvider.values())
                .filter(featureFlags::isEnabled)
                .toList();
    }
}
