package com.subscriptionmanager.subscription.support;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class SupportEmailTemplateRegistry {

    private static final String DEFAULT_SUPPORT_EMAIL = "support@service.com";

    private static final SupportEmailTemplate FALLBACK_TEMPLATE = new SupportEmailTemplate(
            DEFAULT_SUPPORT_EMAIL,
            "Request to cancel {serviceName} subscription",
            """
            Hello Support Team,

            Please cancel my subscription to {serviceName}.

            Account email: {accountEmail}
            Name (if available): {userName}
            Current charge: {amount} {currency}
            Billing period: {billingPeriod}
            Next billing date: {nextBillingDate}

            Please confirm when cancellation is completed.

            Thank you.
            """,
            "Request to pause {serviceName} subscription",
            """
            Hello Support Team,

            Please pause my subscription to {serviceName}.

            Account email: {accountEmail}
            Name (if available): {userName}
            Current charge: {amount} {currency}
            Billing period: {billingPeriod}
            Next billing date: {nextBillingDate}

            Please confirm when pause is completed.

            Thank you.
            """
    );

    private final List<SupportEmailTemplateRule> rules = List.of(
            SupportEmailTemplateRule.byService(
                    "netflix",
                    new SupportEmailTemplate(
                            "support@netflix.com",
                            "Cancel my Netflix subscription",
                            """
                            Hello Netflix Support,

                            I would like to cancel my Netflix subscription.

                            Account email: {accountEmail}
                            Name (if available): {userName}
                            Current charge: {amount} {currency}
                            Billing period: {billingPeriod}
                            Next billing date: {nextBillingDate}

                            Please confirm cancellation by reply.

                            Thank you.
                            """,
                            "Pause my Netflix subscription",
                            """
                            Hello Netflix Support,

                            I would like to pause my Netflix subscription.

                            Account email: {accountEmail}
                            Name (if available): {userName}
                            Current charge: {amount} {currency}
                            Billing period: {billingPeriod}
                            Next billing date: {nextBillingDate}

                            Please confirm pause by reply.

                            Thank you.
                            """
                    )
            ),
            SupportEmailTemplateRule.byService(
                    "spotify",
                    new SupportEmailTemplate(
                            "support@spotify.com",
                            "Cancel my Spotify subscription",
                            """
                            Hello Spotify Support,

                            Please cancel my Spotify subscription.

                            Account email: {accountEmail}
                            Name (if available): {userName}
                            Current charge: {amount} {currency}
                            Billing period: {billingPeriod}
                            Next billing date: {nextBillingDate}

                            Please confirm cancellation by reply.

                            Thank you.
                            """,
                            "Pause my Spotify subscription",
                            """
                            Hello Spotify Support,

                            Please pause my Spotify subscription.

                            Account email: {accountEmail}
                            Name (if available): {userName}
                            Current charge: {amount} {currency}
                            Billing period: {billingPeriod}
                            Next billing date: {nextBillingDate}

                            Please confirm pause by reply.

                            Thank you.
                            """
                    )
            ),
            SupportEmailTemplateRule.byProvider(
                    "BANK_API",
                    new SupportEmailTemplate(
                            "support@bank-api.example",
                            "Cancel recurring payment for {serviceName}",
                            """
                            Hello Support Team,

                            Please cancel recurring payment for {serviceName}.

                            Account email: {accountEmail}
                            Name (if available): {userName}
                            Current charge: {amount} {currency}
                            Billing period: {billingPeriod}
                            Next billing date: {nextBillingDate}

                            Please confirm when cancellation is complete.

                            Thank you.
                            """,
                            "Pause recurring payment for {serviceName}",
                            """
                            Hello Support Team,

                            Please pause recurring payment for {serviceName}.

                            Account email: {accountEmail}
                            Name (if available): {userName}
                            Current charge: {amount} {currency}
                            Billing period: {billingPeriod}
                            Next billing date: {nextBillingDate}

                            Please confirm when pause is complete.

                            Thank you.
                            """
                    )
            )
    );

    public SupportEmailTemplate resolve(String serviceName, String provider) {
        String normalizedServiceName = normalize(serviceName);
        String normalizedProvider = normalize(provider);

        for (SupportEmailTemplateRule rule : rules) {
            if (rule.matches(normalizedServiceName, normalizedProvider)) {
                return rule.template();
            }
        }

        return FALLBACK_TEMPLATE;
    }

    public String defaultSupportEmail() {
        return DEFAULT_SUPPORT_EMAIL;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record SupportEmailTemplate(
            String supportEmail,
            String cancelSubjectTemplate,
            String cancelBodyTemplate,
            String pauseSubjectTemplate,
            String pauseBodyTemplate
    ) {
    }

    private record SupportEmailTemplateRule(
            Set<String> serviceNames,
            Set<String> providers,
            SupportEmailTemplate template
    ) {
        static SupportEmailTemplateRule byService(String serviceName, SupportEmailTemplate template) {
            return new SupportEmailTemplateRule(Set.of(serviceName.toLowerCase(Locale.ROOT)), Set.of(), template);
        }

        static SupportEmailTemplateRule byProvider(String provider, SupportEmailTemplate template) {
            return new SupportEmailTemplateRule(Set.of(), Set.of(provider.toLowerCase(Locale.ROOT)), template);
        }

        boolean matches(String serviceName, String provider) {
            boolean serviceMatches = serviceNames.isEmpty() || serviceNames.contains(serviceName);
            boolean providerMatches = providers.isEmpty() || providers.contains(provider);
            return serviceMatches && providerMatches;
        }
    }
}
