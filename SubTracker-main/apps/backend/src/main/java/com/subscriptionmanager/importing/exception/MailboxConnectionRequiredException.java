package com.subscriptionmanager.importing.exception;

import com.subscriptionmanager.common.enums.ImportProvider;

public class MailboxConnectionRequiredException extends RuntimeException {
    public static final String ERROR_CODE = "MAILBOX_CONNECTION_REQUIRED";

    private final ImportProvider provider;

    public MailboxConnectionRequiredException(ImportProvider provider) {
        super("real mailbox connection is required before import for provider " + provider.name());
        this.provider = provider;
    }

    public ImportProvider getProvider() {
        return provider;
    }
}
