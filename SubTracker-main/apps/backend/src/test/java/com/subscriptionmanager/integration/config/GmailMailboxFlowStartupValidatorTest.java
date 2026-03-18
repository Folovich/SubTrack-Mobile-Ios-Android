package com.subscriptionmanager.integration.config;

import com.subscriptionmanager.importing.config.ImportProviderFeatureFlagsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailMailboxFlowStartupValidatorTest {

    @Test
    void doesNotFailWhenMailboxFlowIsDisabled() {
        ImportProviderFeatureFlagsProperties flags = featureFlags(true, false);
        GmailIntegrationProperties properties = new GmailIntegrationProperties();

        GmailMailboxFlowStartupValidator validator = new GmailMailboxFlowStartupValidator(
                flags,
                properties,
                "",
                "",
                true
        );

        assertDoesNotThrow(validator::validateConfiguration);
    }

    @Test
    void failsFastWhenMailboxFlowEnabledAndConfigMissing() {
        ImportProviderFeatureFlagsProperties flags = featureFlags(true, true);
        GmailIntegrationProperties properties = new GmailIntegrationProperties();

        GmailMailboxFlowStartupValidator validator = new GmailMailboxFlowStartupValidator(
                flags,
                properties,
                "",
                "",
                true
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validateConfiguration);
        assertTrue(exception.getMessage().contains("GMAIL_CLIENT_ID"));
        assertTrue(exception.getMessage().contains("GMAIL_CLIENT_SECRET"));
        assertTrue(exception.getMessage().contains("GMAIL_REDIRECT_URI"));
        assertTrue(exception.getMessage().contains("INTEGRATIONS_TOKEN_ENCRYPTION_SECRET"));
        assertTrue(exception.getMessage().contains("INTEGRATIONS_OAUTH_STATE_SECRET"));
    }

    @Test
    void passesWhenMailboxFlowEnabledAndConfigProvided() {
        ImportProviderFeatureFlagsProperties flags = featureFlags(true, true);
        GmailIntegrationProperties properties = new GmailIntegrationProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri("http://localhost:8080/api/v1/integrations/GMAIL/oauth/callback");
        properties.setFrontendRedirectUri("http://localhost:5173/import");

        GmailMailboxFlowStartupValidator validator = new GmailMailboxFlowStartupValidator(
                flags,
                properties,
                "token-encryption-secret",
                "oauth-state-secret",
                true
        );

        assertDoesNotThrow(validator::validateConfiguration);
    }

    @Test
    void failsWhenMailboxFlowEnabledButProviderDisabled() {
        ImportProviderFeatureFlagsProperties flags = featureFlags(false, true);
        GmailIntegrationProperties properties = new GmailIntegrationProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRedirectUri("http://localhost:8080/api/v1/integrations/GMAIL/oauth/callback");
        properties.setFrontendRedirectUri("http://localhost:5173/import");

        GmailMailboxFlowStartupValidator validator = new GmailMailboxFlowStartupValidator(
                flags,
                properties,
                "token-encryption-secret",
                "oauth-state-secret",
                true
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, validator::validateConfiguration);
        assertTrue(exception.getMessage().contains("IMPORT_PROVIDERS_GMAIL_ENABLED=true"));
    }

    @Test
    void doesNotFailWhenStartupValidationDisabled() {
        ImportProviderFeatureFlagsProperties flags = featureFlags(true, true);
        GmailIntegrationProperties properties = new GmailIntegrationProperties();

        GmailMailboxFlowStartupValidator validator = new GmailMailboxFlowStartupValidator(
                flags,
                properties,
                "",
                "",
                false
        );

        assertDoesNotThrow(validator::validateConfiguration);
    }

    private ImportProviderFeatureFlagsProperties featureFlags(boolean gmailEnabled, boolean mailboxEnabled) {
        ImportProviderFeatureFlagsProperties flags = new ImportProviderFeatureFlagsProperties();
        flags.getGmail().setEnabled(gmailEnabled);
        flags.getGmail().setMailboxEnabled(mailboxEnabled);
        return flags;
    }
}
