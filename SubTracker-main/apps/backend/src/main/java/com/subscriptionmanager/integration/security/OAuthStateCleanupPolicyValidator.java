package com.subscriptionmanager.integration.security;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateCleanupPolicyValidator {
    private final OAuthStateCleanupProperties properties;
    private final Environment environment;

    public OAuthStateCleanupPolicyValidator(
            OAuthStateCleanupProperties properties,
            Environment environment
    ) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validatePolicy() {
        boolean prodProfileActive = environment.acceptsProfiles(Profiles.of("prod", "production"));
        if (prodProfileActive && !properties.isEnabled()) {
            throw new IllegalStateException(
                    "app.integrations.security.oauth-state-cleanup.enabled must be true when profile 'prod' or 'production' is active"
            );
        }
    }
}
