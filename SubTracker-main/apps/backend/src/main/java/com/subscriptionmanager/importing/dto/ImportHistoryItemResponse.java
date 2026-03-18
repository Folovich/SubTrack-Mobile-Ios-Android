package com.subscriptionmanager.importing.dto;

import java.time.OffsetDateTime;

public record ImportHistoryItemResponse(
        Long id,
        String provider,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
