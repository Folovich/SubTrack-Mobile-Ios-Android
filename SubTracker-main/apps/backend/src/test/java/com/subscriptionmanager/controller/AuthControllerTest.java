package com.subscriptionmanager.controller;

import com.subscriptionmanager.dto.AuthResponse;
import com.subscriptionmanager.dto.LoginRequest;
import com.subscriptionmanager.dto.RegisterRequest;
import com.subscriptionmanager.dto.UserResponse;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.exception.LoginThrottledException;
import com.subscriptionmanager.security.ClientIpResolver;
import com.subscriptionmanager.security.LoginProtectionService;
import com.subscriptionmanager.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private LoginProtectionService loginProtectionService;
    private ClientIpResolver clientIpResolver;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        loginProtectionService = Mockito.mock(LoginProtectionService.class);
        clientIpResolver = Mockito.mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        AuthController controller = new AuthController(authService, loginProtectionService, clientIpResolver);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        String body = """
                {
                  "email": "new@subtrack.app",
                  "password": "Passw0rd!X"
                }
                """;

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(sampleAuthResponse("new@subtrack.app"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("new@subtrack.app"));
    }

    @Test
    void loginReturnsOk() throws Exception {
        String body = """
                {
                  "email": "demo@subtrack.app",
                  "password": "Passw0rd!"
                }
                """;

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(sampleAuthResponse("demo@subtrack.app"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("demo@subtrack.app"));
    }

    @Test
    void registerReturnsBadRequestForValidationErrors() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        String body = """
                {
                  "email": "demo@subtrack.app",
                  "password": "wrong-pass"
                }
                """;

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void loginReturnsTooManyRequestsWhenThrottled() throws Exception {
        String body = """
                {
                  "email": "demo@subtrack.app",
                  "password": "Passw0rd!"
                }
                """;

        Mockito.doThrow(new LoginThrottledException(60))
                .when(loginProtectionService)
                .validateLoginAttempt(any(String.class), any(String.class));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again later."))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void registerReturnsBadRequestForWeakPassword() throws Exception {
        String body = """
                {
                  "email": "new@subtrack.app",
                  "password": "lowercase1!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password")
                        .value("Password must include lowercase, uppercase, digit, special character, and contain no spaces"));
    }

    private AuthResponse sampleAuthResponse(String email) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail(email);

        return new AuthResponse("jwt-token", userResponse);
    }
}
