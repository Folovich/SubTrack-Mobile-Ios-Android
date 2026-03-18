package com.subscriptionmanager.contract;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.importing.controller.ImportController;
import com.subscriptionmanager.importing.dto.ImportErrorItemResponse;
import com.subscriptionmanager.importing.dto.ImportItemResultResponse;
import com.subscriptionmanager.importing.dto.ImportResultResponse;
import com.subscriptionmanager.importing.exception.MailboxConnectionRequiredException;
import com.subscriptionmanager.importing.exception.MailboxReauthRequiredException;
import com.subscriptionmanager.importing.service.ImportService;
import com.subscriptionmanager.integration.controller.IntegrationController;
import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;
import com.subscriptionmanager.integration.dto.OAuthStartResponse;
import com.subscriptionmanager.integration.service.IntegrationService;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GmailMailboxFlowContractTest {

    private IntegrationService integrationService;
    private ImportService importService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        integrationService = Mockito.mock(IntegrationService.class);
        importService = Mockito.mock(ImportService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        IntegrationController integrationController = new IntegrationController(integrationService, currentUserService);
        ImportController importController = new ImportController(importService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(integrationController, importController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void oauthStartContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(integrationService.startOAuth(1L, "GMAIL"))
                .thenReturn(new OAuthStartResponse("GMAIL", "https://accounts.google.com/o/oauth2/v2/auth?state=abc"));

        mockMvc.perform(post("/api/v1/integrations/GMAIL/oauth/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.authorizationUrl")
                        .value("https://accounts.google.com/o/oauth2/v2/auth?state=abc"));
    }

    @Test
    void oauthStartFeatureFlagOffErrorShapeContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(integrationService.startOAuth(1L, "GMAIL"))
                .thenThrow(new ApiException("provider GMAIL mailbox import flow is disabled by feature flag"));

        mockMvc.perform(post("/api/v1/integrations/GMAIL/oauth/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider GMAIL mailbox import flow is disabled by feature flag"));
    }

    @Test
    void oauthCallbackSuccessContract() throws Exception {
        when(integrationService.handleOAuthCallback("GMAIL", "auth-code", "state-token", null))
                .thenReturn("http://localhost:5173/import?gmail=connected");

        mockMvc.perform(get("/api/v1/integrations/GMAIL/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "state-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/import?gmail=connected"));
    }

    @Test
    void oauthCallbackInvalidStateContract() throws Exception {
        when(integrationService.handleOAuthCallback("GMAIL", "auth-code", "bad-state", null))
                .thenReturn("http://localhost:5173/import?gmail=error&reason=invalid_state");

        mockMvc.perform(get("/api/v1/integrations/GMAIL/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "bad-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/import?gmail=error&reason=invalid_state"));
    }

    @Test
    void oauthCallbackInvalidCodeContract() throws Exception {
        when(integrationService.handleOAuthCallback("GMAIL", null, "state-token", null))
                .thenReturn("http://localhost:5173/import?gmail=error&reason=missing_code");

        mockMvc.perform(get("/api/v1/integrations/GMAIL/oauth/callback")
                        .param("state", "state-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/import?gmail=error&reason=missing_code"));
    }

    @Test
    void oauthCallbackFeatureFlagOffErrorShapeContract() throws Exception {
        when(integrationService.handleOAuthCallback("GMAIL", "auth-code", "state-token", null))
                .thenThrow(new ApiException("provider GMAIL mailbox import flow is disabled by feature flag"));

        mockMvc.perform(get("/api/v1/integrations/GMAIL/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "state-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider GMAIL mailbox import flow is disabled by feature flag"));
    }

    @Test
    void disconnectContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(integrationService.disconnect(1L, "GMAIL"))
                .thenReturn(new IntegrationConnectionResponse(
                        11L,
                        "GMAIL",
                        "REVOKED",
                        null,
                        OffsetDateTime.parse("2026-03-11T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-11T11:00:00Z"),
                        OffsetDateTime.parse("2026-03-10T09:00:00Z"),
                        null,
                        null
                ));

        mockMvc.perform(post("/api/v1/integrations/GMAIL/disconnect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void syncMailboxContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.syncMailbox(1L, "GMAIL")).thenReturn(sampleImportResult());

        mockMvc.perform(post("/api/v1/imports/GMAIL/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(42))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$.processed").value(2))
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").value(1))
                .andExpect(jsonPath("$.items[0].externalId").value("gmail-msg-001"))
                .andExpect(jsonPath("$.items[0].status").value("IMPORTED"))
                .andExpect(jsonPath("$.items[1].status").value("PARSE_ERROR"));
    }

    @Test
    void syncMailboxNotConnectedErrorShapeContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.syncMailbox(1L, "GMAIL"))
                .thenThrow(new MailboxConnectionRequiredException(ImportProvider.GMAIL));

        mockMvc.perform(post("/api/v1/imports/GMAIL/sync"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAILBOX_CONNECTION_REQUIRED"))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.message")
                        .value("real mailbox connection is required before import for provider GMAIL"));
    }

    @Test
    void syncMailboxReauthRequiredErrorShapeContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.syncMailbox(1L, "GMAIL"))
                .thenThrow(new MailboxReauthRequiredException(ImportProvider.GMAIL));

        mockMvc.perform(post("/api/v1/imports/GMAIL/sync"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAILBOX_REAUTH_REQUIRED"))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.message")
                        .value("gmail access expired or was revoked for provider GMAIL. Reconnect Gmail and retry."));
    }

    @Test
    void syncMailboxFeatureFlagOffErrorShapeContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.syncMailbox(1L, "GMAIL"))
                .thenThrow(new ApiException("provider GMAIL mailbox import flow is disabled by feature flag"));

        mockMvc.perform(post("/api/v1/imports/GMAIL/sync"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider GMAIL mailbox import flow is disabled by feature flag"));
    }

    private ImportResultResponse sampleImportResult() {
        return new ImportResultResponse(
                42L,
                "GMAIL",
                "COMPLETED_WITH_ERRORS",
                2,
                1,
                0,
                1,
                OffsetDateTime.parse("2026-03-11T10:00:00Z"),
                OffsetDateTime.parse("2026-03-11T10:00:01Z"),
                List.of(new ImportErrorItemResponse(
                        "gmail-msg-002",
                        "parser could not extract next billing date (expected YYYY-MM-DD)"
                )),
                List.of(
                        new ImportItemResultResponse(
                                "gmail-msg-001",
                                "IMPORTED",
                                null,
                                "netflix.com",
                                "Netflix",
                                new BigDecimal("9.99"),
                                "USD",
                                "MONTHLY",
                                LocalDate.parse("2026-03-20"),
                                "Entertainment",
                                OffsetDateTime.parse("2026-03-11T09:50:00Z")
                        ),
                        new ImportItemResultResponse(
                                "gmail-msg-002",
                                "PARSE_ERROR",
                                "parser could not extract next billing date (expected YYYY-MM-DD)",
                                "netflix.com",
                                "Netflix",
                                null,
                                null,
                                null,
                                null,
                                "Entertainment",
                                OffsetDateTime.parse("2026-03-11T09:51:00Z")
                        )
                )
        );
    }
}
