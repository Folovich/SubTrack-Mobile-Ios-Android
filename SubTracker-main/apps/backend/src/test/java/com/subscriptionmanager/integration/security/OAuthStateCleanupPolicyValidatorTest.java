package com.subscriptionmanager.integration.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthStateCleanupPolicyValidatorTest {

    @Test
    void validatePolicyAllowsDisabledCleanupOutsideProd() {
        OAuthStateCleanupProperties properties = new OAuthStateCleanupProperties();
        properties.setEnabled(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        OAuthStateCleanupPolicyValidator validator = new OAuthStateCleanupPolicyValidator(properties, environment);

        assertDoesNotThrow(validator::validatePolicy);
    }

    @Test
    void validatePolicyRejectsDisabledCleanupInProd() {
        OAuthStateCleanupProperties properties = new OAuthStateCleanupProperties();
        properties.setEnabled(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        OAuthStateCleanupPolicyValidator validator = new OAuthStateCleanupPolicyValidator(properties, environment);

        assertThrows(IllegalStateException.class, validator::validatePolicy);
    }

    @Test
    void validatePolicyAllowsEnabledCleanupInProd() {
        OAuthStateCleanupProperties properties = new OAuthStateCleanupProperties();
        properties.setEnabled(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        OAuthStateCleanupPolicyValidator validator = new OAuthStateCleanupPolicyValidator(properties, environment);

        assertDoesNotThrow(validator::validatePolicy);
    }

    @Test
    void validatePolicyRejectsDisabledCleanupInProductionAlias() {
        OAuthStateCleanupProperties properties = new OAuthStateCleanupProperties();
        properties.setEnabled(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        OAuthStateCleanupPolicyValidator validator = new OAuthStateCleanupPolicyValidator(properties, environment);

        assertThrows(IllegalStateException.class, validator::validatePolicy);
    }
}
