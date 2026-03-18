package com.subscriptionmanager.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String UNKNOWN_IP = "unknown";

    public String resolve(HttpServletRequest request) {
        String forwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return UNKNOWN_IP;
        }
        return remoteAddr.trim();
    }

    private String firstHeaderValue(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] values = headerValue.split(",", 2);
        String first = values[0].trim();
        return first.isEmpty() ? null : first;
    }
}
