package com.subscriptionmanager.subscription.dto;

public record SupportEmailDraftDetailsResponse(
        String to,
        String subject,
        String body,
        String mailtoUrl,
        String plainTextForCopy
) {
}
