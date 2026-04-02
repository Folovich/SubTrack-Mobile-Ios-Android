package com.subscriptionmanager.integration.provider;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MailImportProviderRegistry {
    private final Map<ImportProvider, MailImportProvider> providers = new EnumMap<>(ImportProvider.class);

    public MailImportProviderRegistry(List<MailImportProvider> providers) {
        for (MailImportProvider provider : providers) {
            this.providers.put(provider.provider(), provider);
        }
    }

    public MailImportProvider require(ImportProvider provider) {
        MailImportProvider mailImportProvider = providers.get(provider);
        if (mailImportProvider == null) {
            throw new ApiException("provider " + provider.name() + " is enabled but no mail provider is registered");
        }
        return mailImportProvider;
    }
}
