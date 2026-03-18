package com.subscriptionmanager.controller;

import com.subscriptionmanager.dto.AuthResponse;
import com.subscriptionmanager.dto.LoginRequest;
import com.subscriptionmanager.dto.RegisterRequest;
import com.subscriptionmanager.dto.UserResponse;
import com.subscriptionmanager.security.ClientIpResolver;
import com.subscriptionmanager.security.CustomUserDetailsService;
import com.subscriptionmanager.security.JwtAuthenticationFilter;
import com.subscriptionmanager.security.LoginProtectionService;
import com.subscriptionmanager.security.SecurityConfig;
import com.subscriptionmanager.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private LoginProtectionService loginProtectionService;

    @MockBean
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
    }

    @Test
    void loginWithoutJwtIsAccessibleOnCanonicalRoute() throws Exception {
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
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void registerWithoutJwtIsAccessibleOnCanonicalRoute() throws Exception {
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
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    private AuthResponse sampleAuthResponse(String email) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail(email);

        return new AuthResponse("jwt-token", userResponse);
    }
}
