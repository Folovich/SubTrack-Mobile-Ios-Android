package com.subscriptionmanager.importing.exception;

import com.subscriptionmanager.common.enums.ImportProvider;

public class ImportConsentRequiredException extends RuntimeException {
    public static final String ERROR_CODE = "IMPORT_CONSENT_REQUIRED";

    private final ImportProvider provider;

    public ImportConsentRequiredException(ImportProvider provider) {
        super("active consent is required before import for provider " + provider.name());
        this.provider = provider;
    }

    public ImportProvider getProvider() {
        return provider;
    }
}
