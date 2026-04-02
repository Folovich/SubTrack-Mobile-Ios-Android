package com.subscriptionmanager.integration.security;

import com.subscriptionmanager.exception.ApiException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final IntegrationKeyResolver keyResolver;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenEncryptionService(IntegrationKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    public String encrypt(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keyResolver.tokenEncryptionKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new ApiException("failed to encrypt integration token");
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - IV_LENGTH];

            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyResolver.tokenEncryptionKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ApiException("failed to decrypt integration token");
        }
    }
}
