package com.subscriptionmanager.integration.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class IntegrationKeyResolver {
    private final SecretKey tokenEncryptionKey;
    private final SecretKey oauthStateSigningKey;

    public IntegrationKeyResolver(
            @Value("${app.integrations.security.token-encryption-secret:${app.jwt.secret}}") String tokenEncryptionSecret,
            @Value("${app.integrations.security.oauth-state-secret:${app.jwt.secret}}") String oauthStateSecret
    ) {
        this.tokenEncryptionKey = new SecretKeySpec(sha256(tokenEncryptionSecret), "AES");
        this.oauthStateSigningKey = Keys.hmacShaKeyFor(sha256(oauthStateSecret));
    }

    public SecretKey tokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public SecretKey oauthStateSigningKey() {
        return oauthStateSigningKey;
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
