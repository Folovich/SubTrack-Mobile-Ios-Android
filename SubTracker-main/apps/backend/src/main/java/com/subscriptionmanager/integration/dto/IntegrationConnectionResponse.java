package com.subscriptionmanager.integration.dto;

import java.time.OffsetDateTime;

public record IntegrationConnectionResponse(
        Long id,
        String provider,
        String status,
        String externalAccountEmail,
        OffsetDateTime connectedAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastSyncAt,
        String lastErrorCode,
        String lastErrorMessage
) {
}
