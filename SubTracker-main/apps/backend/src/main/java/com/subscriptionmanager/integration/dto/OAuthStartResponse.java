package com.subscriptionmanager.integration.dto;

public record OAuthStartResponse(
        String provider,
        String authorizationUrl
) {
}
