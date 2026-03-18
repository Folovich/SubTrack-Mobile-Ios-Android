package com.subscriptionmanager.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.consent.service.ImportConsentService;
import com.subscriptionmanager.entity.IntegrationConnection;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.importing.dto.MailMessageRequest;
import com.subscriptionmanager.importing.exception.MailboxConnectionRequiredException;
import com.subscriptionmanager.importing.exception.MailboxReauthRequiredException;
import com.subscriptionmanager.importing.provider.ImportProviderAvailabilityPolicy;
import com.subscriptionmanager.integration.config.GmailIntegrationProperties;
import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;
import com.subscriptionmanager.integration.security.OAuthStateTokenService;
import com.subscriptionmanager.integration.security.TokenEncryptionService;
import com.subscriptionmanager.repository.IntegrationConnectionRepository;
import com.subscriptionmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmailImportProvider implements MailImportProvider {
    private static final Logger log = LoggerFactory.getLogger(GmailImportProvider.class);

    private static final String STATUS_NOT_CONNECTED = "NOT_CONNECTED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_REAUTH_REQUIRED = "REAUTH_REQUIRED";
    private static final String CALLBACK_STATUS_CONNECTED = "connected";
    private static final String CALLBACK_STATUS_ERROR = "error";
    private static final String CALLBACK_REASON_INVALID_STATE = "invalid_state";
    private static final String CALLBACK_REASON_EXPIRED_STATE = "expired_state";
    private static final String CALLBACK_REASON_STATE_REPLAY = "state_replay";
    private static final String CALLBACK_REASON_PROVIDER_MISMATCH = "provider_mismatch";
    private static final String CALLBACK_REASON_CODE_EXCHANGE_FAILED = "code_exchange_failed";
    private static final String CALLBACK_REASON_MISSING_CODE = "missing_code";
    private static final String CALLBACK_REASON_ACCESS_DENIED = "access_denied";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,})", Pattern.CASE_INSENSITIVE);
    private static final List<String> SEARCH_KEYWORDS = List.of(
            "billing",
            "renewal",
            "payment",
            "invoice",
            "receipt",
            "subscription",
            "charged",
            "plan"
    );

    private final ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy;
    private final IntegrationConnectionRepository integrationConnectionRepository;
    private final UserRepository userRepository;
    private final ImportConsentService importConsentService;
    private final TokenEncryptionService tokenEncryptionService;
    private final OAuthStateTokenService oauthStateTokenService;
    private final GoogleGmailApiClient googleGmailApiClient;
    private final GmailIntegrationProperties properties;

    public GmailImportProvider(
            ImportProviderAvailabilityPolicy importProviderAvailabilityPolicy,
            IntegrationConnectionRepository integrationConnectionRepository,
            UserRepository userRepository,
            ImportConsentService importConsentService,
            TokenEncryptionService tokenEncryptionService,
            OAuthStateTokenService oauthStateTokenService,
            GoogleGmailApiClient googleGmailApiClient,
            GmailIntegrationProperties properties
    ) {
        this.importProviderAvailabilityPolicy = importProviderAvailabilityPolicy;
        this.integrationConnectionRepository = integrationConnectionRepository;
        this.userRepository = userRepository;
        this.importConsentService = importConsentService;
        this.tokenEncryptionService = tokenEncryptionService;
        this.oauthStateTokenService = oauthStateTokenService;
        this.googleGmailApiClient = googleGmailApiClient;
        this.properties = properties;
    }

    @Override
    public ImportProvider provider() {
        return ImportProvider.GMAIL;
    }

    @Override
    public String buildAuthorizationUrl(Long userId) {
        importProviderAvailabilityPolicy.requireEnabledForMailboxFlow(ImportProvider.GMAIL);
        requireConfigured();
        requireUser(userId);

        String state = oauthStateTokenService.issue(userId, ImportProvider.GMAIL, properties.getStateTtlMinutes());
        return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getScope())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build(true)
                .toUriString();
    }

    @Override
    public String handleAuthorizationCallback(String code, String state, String error) {
        importProviderAvailabilityPolicy.requireEnabledForMailboxFlow(ImportProvider.GMAIL);

        try {
            OAuthStateTokenService.StatePayload payload =
                    oauthStateTokenService.parseAndConsume(state, ImportProvider.GMAIL);

            if (error != null && !error.isBlank()) {
                String reason = classifyProviderError(error);
                log.warn("Gmail OAuth callback returned provider error. reason={}", reason);
                return callbackRedirect(CALLBACK_STATUS_ERROR, reason);
            }
            if (code == null || code.isBlank()) {
                log.warn("Gmail OAuth callback rejected because authorization code is missing");
                return callbackRedirect(CALLBACK_STATUS_ERROR, CALLBACK_REASON_MISSING_CODE);
            }

            requireConfigured();
            GoogleGmailApiClient.TokenResponse tokenResponse = googleGmailApiClient.exchangeCode(code);
            GoogleGmailApiClient.GmailProfile profile = googleGmailApiClient.getProfile(tokenResponse.accessToken());
            User user = requireUser(payload.userId());

            importConsentService.grant(user.getId(), ImportProvider.GMAIL.name());
            IntegrationConnection connection = findOrCreateConnection(user);
            OffsetDateTime now = OffsetDateTime.now();

            connection.setStatus(STATUS_ACTIVE);
            connection.setEncryptedAccessToken(tokenEncryptionService.encrypt(tokenResponse.accessToken()));
            connection.setEncryptedRefreshToken(tokenEncryptionService.encrypt(firstNonBlank(
                    tokenResponse.refreshToken(),
                    decrypt(connection.getEncryptedRefreshToken())
            )));
            connection.setTokenExpiresAt(tokenResponse.expiresInSeconds() <= 0
                    ? null
                    : now.plusSeconds(tokenResponse.expiresInSeconds()));
            connection.setExternalAccountEmail(profile.emailAddress());
            connection.setLastErrorCode(null);
            connection.setLastErrorMessage(null);
            connection.setUpdatedAt(now);
            integrationConnectionRepository.save(connection);

            return callbackRedirect(CALLBACK_STATUS_CONNECTED, null);
        } catch (OAuthStateTokenService.StateValidationException ex) {
            String reason = mapStateReason(ex.reason());
            log.warn("Gmail OAuth callback rejected due to state validation failure. reason={}", reason);
            return callbackRedirect(CALLBACK_STATUS_ERROR, reason);
        } catch (GmailApiClientException ex) {
            log.error("Gmail OAuth code exchange failed. statusCode={}, errorCode={}",
                    ex.getStatusCode(), safeErrorCode(ex.getErrorCode()));
            return callbackRedirect(CALLBACK_STATUS_ERROR, CALLBACK_REASON_CODE_EXCHANGE_FAILED);
        } catch (ApiException ex) {
            log.warn("Gmail OAuth callback failed before completion. reason={}", CALLBACK_REASON_CODE_EXCHANGE_FAILED);
            return callbackRedirect(CALLBACK_STATUS_ERROR, CALLBACK_REASON_CODE_EXCHANGE_FAILED);
        } catch (RuntimeException ex) {
            log.error("Unexpected Gmail OAuth callback failure. type={}", ex.getClass().getSimpleName());
            return callbackRedirect(CALLBACK_STATUS_ERROR, CALLBACK_REASON_CODE_EXCHANGE_FAILED);
        }
    }

    @Override
    public IntegrationConnectionResponse connectionStatus(Long userId) {
        importProviderAvailabilityPolicy.requireEnabledForImport(ImportProvider.GMAIL);
        return integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(userId, ImportProvider.GMAIL)
                .map(this::toResponse)
                .orElseGet(() -> new IntegrationConnectionResponse(
                        null,
                        ImportProvider.GMAIL.name(),
                        STATUS_NOT_CONNECTED,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
    }

    @Override
    public IntegrationConnectionResponse disconnect(Long userId) {
        importProviderAvailabilityPolicy.requireEnabledForImport(ImportProvider.GMAIL);
        Optional<IntegrationConnection> optionalConnection = integrationConnectionRepository
                .findTopByUserIdAndProviderOrderByUpdatedAtDesc(userId, ImportProvider.GMAIL);

        if (optionalConnection.isEmpty()) {
            importConsentService.revoke(userId, ImportProvider.GMAIL.name());
            return connectionStatus(userId);
        }

        IntegrationConnection connection = optionalConnection.get();
        String token = firstNonBlank(
                decrypt(connection.getEncryptedAccessToken()),
                decrypt(connection.getEncryptedRefreshToken())
        );

        if (token != null) {
            try {
                googleGmailApiClient.revokeToken(token);
            } catch (GmailApiClientException ignored) {
                // Disconnect stays best-effort if the token was already invalidated upstream.
            }
        }

        clearTokens(connection);
        connection.setStatus(STATUS_REVOKED);
        connection.setLastErrorCode(null);
        connection.setLastErrorMessage(null);
        connection.setUpdatedAt(OffsetDateTime.now());
        integrationConnectionRepository.save(connection);
        importConsentService.revoke(userId, ImportProvider.GMAIL.name());
        return toResponse(connection);
    }

    @Override
    public MailboxFetchResult fetchMailboxMessages(Long userId) {
        importProviderAvailabilityPolicy.requireEnabledForMailboxFlow(ImportProvider.GMAIL);
        IntegrationConnection connection = requireActiveConnection(userId);
        String accessToken = ensureValidAccessToken(connection);
        OffsetDateTime fetchedAt = OffsetDateTime.now();
        String query = buildQuery(connection.getLastSyncAt());

        List<GoogleGmailApiClient.GmailMessageSummary> summaries = executeWithReauthRetry(
                connection,
                accessToken,
                token -> googleGmailApiClient.listMessages(token, query, properties.getMaxResults())
        );
        accessToken = firstNonBlank(decrypt(connection.getEncryptedAccessToken()), accessToken);

        List<MailMessageRequest> messages = new ArrayList<>();
        for (GoogleGmailApiClient.GmailMessageSummary summary : summaries) {
            GoogleGmailApiClient.GmailMessage message = executeWithReauthRetry(
                    connection,
                    accessToken,
                    token -> googleGmailApiClient.getMessage(token, summary.id())
            );
            MailMessageRequest mapped = toMailMessage(message);
            if (mapped == null) {
                continue;
            }
            if (connection.getLastSyncAt() != null && !mapped.receivedAt().isAfter(connection.getLastSyncAt())) {
                continue;
            }
            messages.add(mapped);
        }

        connection.setLastErrorCode(null);
        connection.setLastErrorMessage(null);
        connection.setUpdatedAt(OffsetDateTime.now());
        integrationConnectionRepository.save(connection);
        return new MailboxFetchResult(messages, fetchedAt);
    }

    @Override
    public void markSyncCompleted(Long userId, OffsetDateTime syncedAt) {
        integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(userId, ImportProvider.GMAIL)
                .ifPresent(connection -> {
                    connection.setLastSyncAt(syncedAt);
                    connection.setLastErrorCode(null);
                    connection.setLastErrorMessage(null);
                    connection.setUpdatedAt(OffsetDateTime.now());
                    integrationConnectionRepository.save(connection);
                });
    }

    private <T> T executeWithReauthRetry(
            IntegrationConnection connection,
            String accessToken,
            AuthorizedCall<T> authorizedCall
    ) {
        try {
            return authorizedCall.execute(accessToken);
        } catch (GmailApiClientException ex) {
            if (!isUnauthorized(ex)) {
                throw new ApiException("failed to fetch Gmail mailbox");
            }

            String refreshedToken = refreshAccessToken(connection);
            return authorizedCall.execute(refreshedToken);
        }
    }

    private IntegrationConnection requireActiveConnection(Long userId) {
        IntegrationConnection connection = integrationConnectionRepository
                .findTopByUserIdAndProviderOrderByUpdatedAtDesc(userId, ImportProvider.GMAIL)
                .orElseThrow(() -> new MailboxConnectionRequiredException(ImportProvider.GMAIL));

        if (STATUS_REAUTH_REQUIRED.equals(connection.getStatus())) {
            throw new MailboxReauthRequiredException(ImportProvider.GMAIL);
        }
        if (!STATUS_ACTIVE.equals(connection.getStatus())) {
            throw new MailboxConnectionRequiredException(ImportProvider.GMAIL);
        }

        return connection;
    }

    private String ensureValidAccessToken(IntegrationConnection connection) {
        String accessToken = decrypt(connection.getEncryptedAccessToken());
        if (accessToken != null
                && connection.getTokenExpiresAt() != null
                && connection.getTokenExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(1))) {
            return accessToken;
        }
        return refreshAccessToken(connection);
    }

    private String refreshAccessToken(IntegrationConnection connection) {
        String refreshToken = decrypt(connection.getEncryptedRefreshToken());
        if (refreshToken == null || refreshToken.isBlank()) {
            markReauthRequired(connection, "MISSING_REFRESH_TOKEN", "gmail refresh token is missing");
            throw new MailboxReauthRequiredException(ImportProvider.GMAIL);
        }

        try {
            GoogleGmailApiClient.TokenResponse tokenResponse = googleGmailApiClient.refreshAccessToken(refreshToken);
            connection.setEncryptedAccessToken(tokenEncryptionService.encrypt(tokenResponse.accessToken()));
            connection.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds()));
            connection.setStatus(STATUS_ACTIVE);
            connection.setLastErrorCode(null);
            connection.setLastErrorMessage(null);
            connection.setUpdatedAt(OffsetDateTime.now());
            integrationConnectionRepository.save(connection);
            return tokenResponse.accessToken();
        } catch (GmailApiClientException ex) {
            if (isRefreshRevoked(ex)) {
                markReauthRequired(connection, "TOKEN_REVOKED", "gmail access was revoked");
                throw new MailboxReauthRequiredException(ImportProvider.GMAIL);
            }
            throw new ApiException("failed to refresh Gmail access token");
        }
    }

    private MailMessageRequest toMailMessage(GoogleGmailApiClient.GmailMessage message) {
        String fromHeader = header(message.headers(), "From");
        String email = extractEmail(fromHeader);
        if (email == null) {
            return null;
        }

        OffsetDateTime receivedAt = toReceivedAt(message.internalDate());
        String subject = firstNonBlank(header(message.headers(), "Subject"), "(no subject)");
        String body = firstNonBlank(extractBody(message.payload()), message.snippet(), subject);
        return new MailMessageRequest(
                message.id(),
                email,
                subject,
                body,
                receivedAt
        );
    }

    private OffsetDateTime toReceivedAt(String internalDate) {
        if (internalDate == null || internalDate.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        long timestampMs = Long.parseLong(internalDate);
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestampMs), ZoneOffset.UTC);
    }

    private String extractBody(JsonNode payload) {
        List<String> plainBodies = new ArrayList<>();
        List<String> htmlBodies = new ArrayList<>();
        collectBodies(payload, plainBodies, htmlBodies);

        if (!plainBodies.isEmpty()) {
            return String.join("\n", plainBodies);
        }
        if (!htmlBodies.isEmpty()) {
            return stripHtml(String.join("\n", htmlBodies));
        }
        return null;
    }

    private void collectBodies(JsonNode part, List<String> plainBodies, List<String> htmlBodies) {
        if (part == null || part.isMissingNode()) {
            return;
        }

        String mimeType = part.path("mimeType").asText("");
        String data = part.path("body").path("data").asText("");
        if (!data.isBlank()) {
            String decoded = decodeBody(data);
            if ("text/plain".equalsIgnoreCase(mimeType)) {
                plainBodies.add(decoded);
            } else if ("text/html".equalsIgnoreCase(mimeType)) {
                htmlBodies.add(decoded);
            }
        }

        for (JsonNode nestedPart : part.path("parts")) {
            collectBodies(nestedPart, plainBodies, htmlBodies);
        }
    }

    private String decodeBody(String encodedData) {
        byte[] decoded = Base64.getUrlDecoder().decode(encodedData);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String stripHtml(String html) {
        return html
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractEmail(String fromHeader) {
        if (fromHeader == null || fromHeader.isBlank()) {
            return null;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(fromHeader);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }

        String normalized = fromHeader.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("@") ? normalized : null;
    }

    private String buildQuery(OffsetDateTime lastSyncAt) {
        OffsetDateTime searchStart = lastSyncAt != null
                ? lastSyncAt.minusDays(1)
                : OffsetDateTime.now().minusDays(properties.getInitialLookbackDays());

        String keywords = "(" + String.join(" OR ", SEARCH_KEYWORDS) + ")";
        String afterDate = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(searchStart.toLocalDate());
        return "in:inbox " + keywords + " after:" + afterDate;
    }

    private String classifyProviderError(String providerError) {
        String normalized = providerError.trim().toLowerCase(Locale.ROOT);
        if ("access_denied".equals(normalized)) {
            return CALLBACK_REASON_ACCESS_DENIED;
        }
        return CALLBACK_REASON_CODE_EXCHANGE_FAILED;
    }

    private String mapStateReason(OAuthStateTokenService.StateErrorReason reason) {
        return switch (reason) {
            case EXPIRED_STATE -> CALLBACK_REASON_EXPIRED_STATE;
            case STATE_REPLAY -> CALLBACK_REASON_STATE_REPLAY;
            case PROVIDER_MISMATCH -> CALLBACK_REASON_PROVIDER_MISMATCH;
            case INVALID_STATE -> CALLBACK_REASON_INVALID_STATE;
        };
    }

    private String safeErrorCode(String rawErrorCode) {
        if (rawErrorCode == null || rawErrorCode.isBlank()) {
            return "unknown";
        }
        return rawErrorCode.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private IntegrationConnectionResponse toResponse(IntegrationConnection connection) {
        return new IntegrationConnectionResponse(
                connection.getId(),
                connection.getProvider().name(),
                connection.getStatus(),
                connection.getExternalAccountEmail(),
                connection.getCreatedAt(),
                connection.getUpdatedAt(),
                connection.getLastSyncAt(),
                connection.getLastErrorCode(),
                connection.getLastErrorMessage()
        );
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private IntegrationConnection findOrCreateConnection(User user) {
        return integrationConnectionRepository.findTopByUserIdAndProviderOrderByUpdatedAtDesc(user.getId(), ImportProvider.GMAIL)
                .orElseGet(() -> {
                    IntegrationConnection connection = new IntegrationConnection();
                    connection.setUser(user);
                    connection.setProvider(ImportProvider.GMAIL);
                    connection.setStatus(STATUS_NOT_CONNECTED);
                    connection.setCreatedAt(OffsetDateTime.now());
                    connection.setUpdatedAt(OffsetDateTime.now());
                    return connection;
                });
    }

    private void clearTokens(IntegrationConnection connection) {
        connection.setEncryptedAccessToken(null);
        connection.setEncryptedRefreshToken(null);
        connection.setTokenExpiresAt(null);
    }

    private void markReauthRequired(IntegrationConnection connection, String errorCode, String errorMessage) {
        clearTokens(connection);
        connection.setStatus(STATUS_REAUTH_REQUIRED);
        connection.setLastErrorCode(errorCode);
        connection.setLastErrorMessage(errorMessage);
        connection.setUpdatedAt(OffsetDateTime.now());
        integrationConnectionRepository.save(connection);
    }

    private boolean isUnauthorized(GmailApiClientException ex) {
        return ex.getStatusCode() == 401;
    }

    private boolean isRefreshRevoked(GmailApiClientException ex) {
        String errorCode = ex.getErrorCode();
        return ex.getStatusCode() == 400
                && ("invalid_grant".equalsIgnoreCase(errorCode)
                || "invalid_token".equalsIgnoreCase(errorCode));
    }

    private String callbackRedirect(String status, String reason) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getFrontendRedirectUri())
                .queryParam("gmail", status);
        if (reason != null && !reason.isBlank()) {
            builder.queryParam("reason", reason);
        }
        return builder.build(true).toUriString();
    }

    private String decrypt(String encryptedToken) {
        return encryptedToken == null ? null : tokenEncryptionService.decrypt(encryptedToken);
    }

    private void requireConfigured() {
        List<String> missing = new ArrayList<>();
        if (isBlank(properties.getClientId())) {
            missing.add("GMAIL_CLIENT_ID");
        }
        if (isBlank(properties.getClientSecret())) {
            missing.add("GMAIL_CLIENT_SECRET");
        }
        if (isBlank(properties.getRedirectUri())) {
            missing.add("GMAIL_REDIRECT_URI");
        }
        if (isBlank(properties.getFrontendRedirectUri())) {
            missing.add("GMAIL_FRONTEND_REDIRECT_URI");
        }
        if (!missing.isEmpty()) {
            throw new ApiException("gmail oauth is not configured; set: " + String.join(", ", missing));
        }
    }

    private String header(Map<String, String> headers, String name) {
        return headers.get(name);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface AuthorizedCall<T> {
        T execute(String accessToken);
    }
}
