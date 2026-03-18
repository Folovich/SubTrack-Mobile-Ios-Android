package com.subscriptionmanager.subscription.dto;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpcomingSubscriptionResponse(
        Long id,
        String serviceName,
        String category,
        BigDecimal amount,
        String currency,
        BillingPeriod billingPeriod,
        LocalDate nextBillingDate,
        SubscriptionStatus status,
        long daysUntilBilling
) {
}
