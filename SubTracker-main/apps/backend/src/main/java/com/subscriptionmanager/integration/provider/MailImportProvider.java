package com.subscriptionmanager.integration.provider;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.importing.dto.MailMessageRequest;
import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface MailImportProvider {
    ImportProvider provider();
    String buildAuthorizationUrl(Long userId);
    String handleAuthorizationCallback(String code, String state, String error);
    IntegrationConnectionResponse connectionStatus(Long userId);
    IntegrationConnectionResponse disconnect(Long userId);
    MailboxFetchResult fetchMailboxMessages(Long userId);
    void markSyncCompleted(Long userId, OffsetDateTime syncedAt);

    record MailboxFetchResult(
            List<MailMessageRequest> messages,
            OffsetDateTime fetchedAt
    ) {
    }
}
