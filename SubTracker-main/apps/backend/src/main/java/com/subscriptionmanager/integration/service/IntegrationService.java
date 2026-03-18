package com.subscriptionmanager.integration.service;

import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;
import com.subscriptionmanager.integration.dto.OAuthStartResponse;

import java.util.List;

public interface IntegrationService {
    List<IntegrationConnectionResponse> list(Long userId);
    IntegrationConnectionResponse status(Long userId, String providerRaw);
    OAuthStartResponse startOAuth(Long userId, String providerRaw);
    String handleOAuthCallback(String providerRaw, String code, String state, String error);
    IntegrationConnectionResponse disconnect(Long userId, String providerRaw);
}
