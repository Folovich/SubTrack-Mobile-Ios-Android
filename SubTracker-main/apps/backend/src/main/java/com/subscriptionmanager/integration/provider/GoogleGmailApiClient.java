package com.subscriptionmanager.integration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.subscriptionmanager.integration.config.GmailIntegrationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoogleGmailApiClient {
    private final RestClient restClient;
    private final GmailIntegrationProperties properties;

    @Autowired
    public GoogleGmailApiClient(GmailIntegrationProperties properties) {
        this(RestClient.builder().build(), properties);
    }

    GoogleGmailApiClient(RestClient restClient, GmailIntegrationProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("redirect_uri", properties.getRedirectUri());
        body.add("grant_type", "authorization_code");
        return tokenRequest(body);
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("grant_type", "refresh_token");
        return tokenRequest(body);
    }

    public void revokeToken(String token) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);

        try {
            restClient.post()
                    .uri(properties.getRevokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw toClientException(ex, "gmail revoke failed");
        }
    }

    public GmailProfile getProfile(String accessToken) {
        JsonNode node = authorizedGet(
                accessToken,
                properties.getGmailApiBaseUrl() + "/users/me/profile"
        );
        return new GmailProfile(
                text(node, "emailAddress"),
                text(node, "historyId")
        );
    }

    public List<GmailMessageSummary> listMessages(String accessToken, String query, int maxResults) {
        List<GmailMessageSummary> messages = new ArrayList<>();
        String pageToken = null;

        while (messages.size() < maxResults) {
            int pageSize = Math.min(100, maxResults - messages.size());
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(properties.getGmailApiBaseUrl() + "/users/me/messages")
                    .queryParam("q", query)
                    .queryParam("maxResults", pageSize);
            if (pageToken != null) {
                builder.queryParam("pageToken", pageToken);
            }

            JsonNode node = authorizedGet(accessToken, builder.build(false).toUriString());
            for (JsonNode messageNode : node.path("messages")) {
                messages.add(new GmailMessageSummary(
                        text(messageNode, "id"),
                        text(messageNode, "threadId")
                ));
            }

            pageToken = text(node, "nextPageToken");
            if (pageToken == null || pageToken.isBlank()) {
                break;
            }
        }

        return messages;
    }

    public GmailMessage getMessage(String accessToken, String messageId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getGmailApiBaseUrl() + "/users/me/messages/{messageId}")
                .queryParam("format", "full");

        JsonNode node = authorizedGet(accessToken, builder.buildAndExpand(messageId).toUriString());
        return new GmailMessage(
                text(node, "id"),
                text(node, "threadId"),
                text(node, "snippet"),
                text(node, "historyId"),
                text(node, "internalDate"),
                headers(node.path("payload").path("headers")),
                node.path("payload")
        );
    }

    private TokenResponse tokenRequest(MultiValueMap<String, String> body) {
        try {
            JsonNode node = restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return new TokenResponse(
                    text(node, "access_token"),
                    text(node, "refresh_token"),
                    node.path("expires_in").asLong(0)
            );
        } catch (RestClientResponseException ex) {
            throw toClientException(ex, "gmail token request failed");
        }
    }

    private JsonNode authorizedGet(String accessToken, String uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw toClientException(ex, "gmail api request failed");
        }
    }

    private GmailApiClientException toClientException(RestClientResponseException ex, String fallbackMessage) {
        JsonNode responseBody = ex.getResponseBodyAs(JsonNode.class);
        String errorCode = responseBody == null ? null : firstNonBlank(
                text(responseBody, "error"),
                text(responseBody.path("error"), "status"),
                text(responseBody.path("error"), "message")
        );
        String message = responseBody == null ? fallbackMessage : firstNonBlank(
                text(responseBody, "error_description"),
                text(responseBody.path("error"), "message"),
                fallbackMessage
        );
        return new GmailApiClientException(ex.getStatusCode().value(), errorCode, message);
    }

    private Map<String, String> headers(JsonNode headersNode) {
        Map<String, String> headers = new HashMap<>();
        for (JsonNode headerNode : headersNode) {
            String name = text(headerNode, "name");
            String value = text(headerNode, "value");
            if (name != null && value != null) {
                headers.put(name, value);
            }
        }
        return headers;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        String value = valueNode.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresInSeconds
    ) {
    }

    public record GmailProfile(
            String emailAddress,
            String historyId
    ) {
    }

    public record GmailMessageSummary(
            String id,
            String threadId
    ) {
    }

    public record GmailMessage(
            String id,
            String threadId,
            String snippet,
            String historyId,
            String internalDate,
            Map<String, String> headers,
            JsonNode payload
    ) {
    }
}
