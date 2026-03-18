package com.subscriptionmanager.subscription.dto;

import com.subscriptionmanager.subscription.support.SupportEmailAction;
import com.subscriptionmanager.subscription.support.SupportEmailEventType;
import jakarta.validation.constraints.NotNull;

public record SupportEmailEventRequest(
        @NotNull(message = "action is required")
        SupportEmailAction action,
        @NotNull(message = "event is required")
        SupportEmailEventType event
) {
}
