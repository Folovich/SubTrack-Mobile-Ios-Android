package com.subscriptionmanager.importing.dto;

public record ImportErrorItemResponse(
        String externalId,
        String reason
) {
}
