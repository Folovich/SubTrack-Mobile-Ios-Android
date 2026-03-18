package com.subscriptionmanager.consent.dto;

import java.time.OffsetDateTime;

public record ImportConsentStatusResponse(
        String provider,
        String status,
        String scope,
        OffsetDateTime grantedAt,
        OffsetDateTime revokedAt,
        String integrationStatus
) {
}
