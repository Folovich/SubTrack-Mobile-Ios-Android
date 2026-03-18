package com.subscriptionmanager.integration.provider;

public class GmailApiClientException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public GmailApiClientException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
