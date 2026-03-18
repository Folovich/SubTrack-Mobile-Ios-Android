package com.subscriptionmanager.subscription.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupportEmailTemplateRegistryTest {

    private final SupportEmailTemplateRegistry registry = new SupportEmailTemplateRegistry();

    @Test
    void resolvesKnownServiceTemplate() {
        SupportEmailTemplateRegistry.SupportEmailTemplate template = registry.resolve("Netflix", "MANUAL");

        assertEquals("support@netflix.com", template.supportEmail());
        assertEquals("Cancel my Netflix subscription", template.cancelSubjectTemplate());
    }

    @Test
    void resolvesFallbackTemplateForUnknownService() {
        SupportEmailTemplateRegistry.SupportEmailTemplate template = registry.resolve("Unknown Service", "UNKNOWN");

        assertEquals("support@service.com", template.supportEmail());
        assertEquals("Request to cancel {serviceName} subscription", template.cancelSubjectTemplate());
    }
}
