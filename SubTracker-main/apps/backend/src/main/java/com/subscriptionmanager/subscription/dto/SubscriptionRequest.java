package com.subscriptionmanager.subscription.dto;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotBlank
        @Size(max = 255)
        String serviceName,
        Long categoryId,
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,
        @NotBlank
        @Size(min = 3, max = 3)
        String currency,
        @NotNull
        BillingPeriod billingPeriod,
        @NotNull
        LocalDate nextBillingDate,
        SubscriptionStatus status
) {
}
