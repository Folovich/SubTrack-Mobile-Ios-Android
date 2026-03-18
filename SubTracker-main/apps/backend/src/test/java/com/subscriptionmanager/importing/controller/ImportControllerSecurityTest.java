package com.subscriptionmanager.importing.controller;

import com.subscriptionmanager.importing.service.ImportService;
import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.security.CustomUserDetailsService;
import com.subscriptionmanager.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportController.class)
class ImportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportService importService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void historyWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/imports"))
                .andExpect(status().isUnauthorized());
    }
}
