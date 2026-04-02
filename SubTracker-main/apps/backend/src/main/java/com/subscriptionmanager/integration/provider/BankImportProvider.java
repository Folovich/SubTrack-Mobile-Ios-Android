package com.subscriptionmanager.integration.provider;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.importing.dto.MailMessageRequest;
import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface BankImportProvider {
    ImportProvider provider();

    default String buildAuthorizationUrl(Long userId) {
        throw new UnsupportedOperationException("Bank provider does not support OAuth");
    }

    default String handleAuthorizationCallback(String code, String state, String error) {
        throw new UnsupportedOperationException("Bank provider does not support OAuth");
    }

    default IntegrationConnectionResponse connectionStatus(Long userId) {
        throw new UnsupportedOperationException("Bank provider does not support mailbox connections");
    }

    default IntegrationConnectionResponse disconnect(Long userId) {
        throw new UnsupportedOperationException("Bank provider does not support mailbox connections");
    }

    default MailImportProvider.MailboxFetchResult fetchMailboxMessages(Long userId) {
        throw new UnsupportedOperationException("Bank provider does not support mailbox import");
    }

    default void markSyncCompleted(Long userId, OffsetDateTime syncedAt) {
    }
}
