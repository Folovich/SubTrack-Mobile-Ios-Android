package com.subscriptionmanager.importing.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ImportResultResponse(
        Long jobId,
        String provider,
        String status,
        int processed,
        int created,
        int skipped,
        int errors,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<ImportErrorItemResponse> errorItems,
        List<ImportItemResultResponse> items
) {
}
