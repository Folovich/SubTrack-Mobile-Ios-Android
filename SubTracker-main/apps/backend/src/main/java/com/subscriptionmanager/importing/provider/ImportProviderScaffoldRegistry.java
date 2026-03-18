package com.subscriptionmanager.importing.provider;

import com.subscriptionmanager.common.enums.ImportProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ImportProviderScaffoldRegistry {

    private final Map<ImportProvider, ProviderScaffold> scaffolds = new EnumMap<>(ImportProvider.class);

    public ImportProviderScaffoldRegistry() {
        register(ImportProvider.GMAIL, true);
        register(ImportProvider.YANDEX, false);
        register(ImportProvider.MAIL_RU, false);
        register(ImportProvider.BANK_API, false);
    }

    public boolean isImportFlowImplemented(ImportProvider provider) {
        ProviderScaffold scaffold = scaffolds.get(provider);
        return scaffold != null && scaffold.importFlowImplemented();
    }

    private void register(ImportProvider provider, boolean importFlowImplemented) {
        scaffolds.put(provider, new ProviderScaffold(provider, importFlowImplemented));
    }

    private record ProviderScaffold(ImportProvider provider, boolean importFlowImplemented) {
    }
}
