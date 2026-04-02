package com.subscriptionmanager.controller;

import com.subscriptionmanager.dto.AuthResponse;
import com.subscriptionmanager.dto.LoginRequest;
import com.subscriptionmanager.dto.RegisterRequest;
import com.subscriptionmanager.exception.LoginThrottledException;
import com.subscriptionmanager.security.ClientIpResolver;
import com.subscriptionmanager.security.LoginProtectionService;
import com.subscriptionmanager.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginProtectionService loginProtectionService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
            AuthService authService,
            LoginProtectionService loginProtectionService,
            ClientIpResolver clientIpResolver
    ) {
        this.authService = authService;
        this.loginProtectionService = loginProtectionService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String clientIp = clientIpResolver.resolve(httpServletRequest);

        loginProtectionService.validateLoginAttempt(normalizedEmail, clientIp);

        try {
            AuthResponse response = authService.login(request);
            loginProtectionService.recordLoginSuccess(normalizedEmail, clientIp);
            return response;
        } catch (AuthenticationException ex) {
            long retryAfterSeconds = loginProtectionService.recordLoginFailure(normalizedEmail, clientIp);
            if (retryAfterSeconds > 0) {
                throw new LoginThrottledException(retryAfterSeconds);
            }
            throw ex;
        }
    }
}
