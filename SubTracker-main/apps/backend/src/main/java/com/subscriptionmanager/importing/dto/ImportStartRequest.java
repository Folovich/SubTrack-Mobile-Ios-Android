package com.subscriptionmanager.importing.dto;

import com.subscriptionmanager.common.enums.ImportProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportStartRequest(
        @NotNull(message = "provider is required")
        ImportProvider provider,
        @NotEmpty(message = "messages must contain at least one message")
        @Size(max = 100, message = "messages must contain at most 100 items")
        List<@Valid MailMessageRequest> messages
) {
}
