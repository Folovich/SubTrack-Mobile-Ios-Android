package com.subscriptionmanager.importing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record MailMessageRequest(
        @NotBlank(message = "externalId is required")
        @Size(max = 255, message = "externalId must be at most 255 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9._:-]{1,255}$",
                message = "externalId may contain only letters, digits, dot, underscore, colon and hyphen"
        )
        String externalId,
        @NotBlank(message = "from is required")
        @Size(max = 255, message = "from must be at most 255 characters")
        @Email(message = "from must be a valid email address")
        String from,
        @NotBlank(message = "subject is required")
        @Size(max = 500, message = "subject must be at most 500 characters")
        String subject,
        @NotBlank(message = "body is required")
        @Size(max = 10000, message = "body must be at most 10000 characters")
        String body,
        @NotNull(message = "receivedAt is required")
        @PastOrPresent(message = "receivedAt must be in the past or present")
        OffsetDateTime receivedAt
) {
}
