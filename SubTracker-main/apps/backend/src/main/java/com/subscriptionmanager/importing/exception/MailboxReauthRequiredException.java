package com.subscriptionmanager.importing.exception;

import com.subscriptionmanager.common.enums.ImportProvider;

public class MailboxReauthRequiredException extends RuntimeException {
    public static final String ERROR_CODE = "MAILBOX_REAUTH_REQUIRED";

    private final ImportProvider provider;

    public MailboxReauthRequiredException(ImportProvider provider) {
        super("gmail access expired or was revoked for provider " + provider.name() + ". Reconnect Gmail and retry.");
        this.provider = provider;
    }

    public ImportProvider getProvider() {
        return provider;
    }
}
