package com.subscriptionmanager.usage.dto;

public record UsageSignalResponse(
        Long id,
        Long subscriptionId,
        String signalType,
        String value,
        String createdAt
) {
}
