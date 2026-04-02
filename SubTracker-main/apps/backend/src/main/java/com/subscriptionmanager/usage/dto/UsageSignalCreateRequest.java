package com.subscriptionmanager.usage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UsageSignalCreateRequest(
        @NotNull(message = "subscriptionId is required")
        @Positive(message = "subscriptionId must be greater than 0")
        Long subscriptionId,
        @NotBlank(message = "signalType is required")
        @Size(max = 50, message = "signalType must be at most 50 characters")
        String signalType,
        @NotBlank(message = "value is required")
        @Size(max = 255, message = "value must be at most 255 characters")
        String value
) {
}
