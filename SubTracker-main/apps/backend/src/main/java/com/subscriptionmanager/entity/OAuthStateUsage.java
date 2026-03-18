package com.subscriptionmanager.entity;

import com.subscriptionmanager.common.enums.ImportProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "oauth_state_usage")
public class OAuthStateUsage {
    @Id
    @Column(name = "state_jti", nullable = false, length = 64)
    private String stateJti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ImportProvider provider;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at", nullable = false)
    private OffsetDateTime consumedAt;

    public String getStateJti() {
        return stateJti;
    }

    public void setStateJti(String stateJti) {
        this.stateJti = stateJti;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ImportProvider getProvider() {
        return provider;
    }

    public void setProvider(ImportProvider provider) {
        this.provider = provider;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }
}
