package com.subscriptionmanager.importing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ImportItemResultResponse(
        String externalId,
        String status,
        String reason,
        String sourceProvider,
        String serviceName,
        BigDecimal amount,
        String currency,
        String billingPeriod,
        LocalDate nextBillingDate,
        String category,
        OffsetDateTime receivedAt
) {
}
