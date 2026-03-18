package com.subscriptionmanager.integration.security;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.entity.OAuthStateUsage;
import com.subscriptionmanager.repository.OAuthStateUsageRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class OAuthStateTokenService {
    private final IntegrationKeyResolver keyResolver;
    private final OAuthStateUsageRepository oauthStateUsageRepository;

    public OAuthStateTokenService(
            IntegrationKeyResolver keyResolver,
            OAuthStateUsageRepository oauthStateUsageRepository
    ) {
        this.keyResolver = keyResolver;
        this.oauthStateUsageRepository = oauthStateUsageRepository;
    }

    public String issue(Long userId, ImportProvider provider, int ttlMinutes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("provider", provider.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(keyResolver.oauthStateSigningKey())
                .compact();
    }

    @Transactional
    public StatePayload parseAndConsume(String stateToken, ImportProvider expectedProvider) {
        Claims claims = parseClaims(stateToken);
        StatePayload payload = toPayload(claims);
        if (expectedProvider != null && payload.provider() != expectedProvider) {
            throw new StateValidationException(StateErrorReason.PROVIDER_MISMATCH, "oauth provider mismatch");
        }
        consumeState(claims.getId(), claims.getExpiration(), payload);
        return payload;
    }

    private Claims parseClaims(String stateToken) {
        if (stateToken == null || stateToken.isBlank()) {
            throw new StateValidationException(StateErrorReason.INVALID_STATE, "missing oauth state");
        }
        try {
            return Jwts.parser()
                    .verifyWith(keyResolver.oauthStateSigningKey())
                    .build()
                    .parseSignedClaims(stateToken)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new StateValidationException(StateErrorReason.EXPIRED_STATE, "expired oauth state");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new StateValidationException(StateErrorReason.INVALID_STATE, "invalid oauth state");
        }
    }

    private StatePayload toPayload(Claims claims) {
        try {
            return new StatePayload(
                    Long.parseLong(claims.getSubject()),
                    ImportProvider.valueOf(claims.get("provider", String.class))
            );
        } catch (Exception ex) {
            throw new StateValidationException(StateErrorReason.INVALID_STATE, "invalid oauth state payload");
        }
    }

    private void consumeState(String tokenId, Date expiresAt, StatePayload payload) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            throw new StateValidationException(StateErrorReason.INVALID_STATE, "oauth state token metadata is missing");
        }

        OAuthStateUsage usage = new OAuthStateUsage();
        usage.setStateJti(tokenId);
        usage.setUserId(payload.userId());
        usage.setProvider(payload.provider());
        usage.setExpiresAt(OffsetDateTime.ofInstant(expiresAt.toInstant(), ZoneOffset.UTC));
        usage.setConsumedAt(OffsetDateTime.now(ZoneOffset.UTC));

        try {
            oauthStateUsageRepository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException ex) {
            if (oauthStateUsageRepository.existsById(tokenId)) {
                throw new StateValidationException(StateErrorReason.STATE_REPLAY, "oauth state replay detected");
            }
            throw new StateValidationException(StateErrorReason.INVALID_STATE, "failed to persist oauth state");
        }
    }

    public record StatePayload(
            Long userId,
            ImportProvider provider
    ) {
    }

    public enum StateErrorReason {
        INVALID_STATE("invalid_state"),
        EXPIRED_STATE("expired_state"),
        STATE_REPLAY("state_replay"),
        PROVIDER_MISMATCH("provider_mismatch");

        private final String code;

        StateErrorReason(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    public static class StateValidationException extends RuntimeException {
        private final StateErrorReason reason;

        public StateValidationException(StateErrorReason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public StateErrorReason reason() {
            return reason;
        }
    }
}
