package com.subscriptionmanager.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.integrations.gmail")
public class GmailIntegrationProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontendRedirectUri = "http://localhost:5173/import";
    private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String revokeUri = "https://oauth2.googleapis.com/revoke";
    private String gmailApiBaseUrl = "https://gmail.googleapis.com/gmail/v1";
    private String scope = "https://www.googleapis.com/auth/gmail.readonly";
    private int stateTtlMinutes = 10;
    private int maxResults = 50;
    private int initialLookbackDays = 365;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendRedirectUri() {
        return frontendRedirectUri;
    }

    public void setFrontendRedirectUri(String frontendRedirectUri) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getRevokeUri() {
        return revokeUri;
    }

    public void setRevokeUri(String revokeUri) {
        this.revokeUri = revokeUri;
    }

    public String getGmailApiBaseUrl() {
        return gmailApiBaseUrl;
    }

    public void setGmailApiBaseUrl(String gmailApiBaseUrl) {
        this.gmailApiBaseUrl = gmailApiBaseUrl;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getStateTtlMinutes() {
        return stateTtlMinutes;
    }

    public void setStateTtlMinutes(int stateTtlMinutes) {
        this.stateTtlMinutes = stateTtlMinutes;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getInitialLookbackDays() {
        return initialLookbackDays;
    }

    public void setInitialLookbackDays(int initialLookbackDays) {
        this.initialLookbackDays = initialLookbackDays;
    }
}
