package com.subscriptionmanager.subscription.dto;

import com.subscriptionmanager.subscription.support.SupportEmailAction;

public record SupportEmailDraftResponse(
        Long subscriptionId,
        SupportEmailAction action,
        String provider,
        SupportEmailDraftDetailsResponse draft
) {
}
