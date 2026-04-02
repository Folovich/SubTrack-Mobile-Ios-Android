package com.subscriptionmanager.security;

public interface LoginProtectionService {

    void validateLoginAttempt(String email, String clientIp);

    long recordLoginFailure(String email, String clientIp);

    void recordLoginSuccess(String email, String clientIp);
}
