package com.subscriptionmanager.contract;

import com.subscriptionmanager.common.enums.ImportProvider;
import com.subscriptionmanager.consent.controller.ImportConsentController;
import com.subscriptionmanager.consent.dto.ImportConsentStatusResponse;
import com.subscriptionmanager.consent.service.ImportConsentService;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.importing.controller.ImportController;
import com.subscriptionmanager.importing.dto.ImportErrorItemResponse;
import com.subscriptionmanager.importing.dto.ImportHistoryItemResponse;
import com.subscriptionmanager.importing.dto.ImportItemResultResponse;
import com.subscriptionmanager.importing.dto.ImportResultResponse;
import com.subscriptionmanager.importing.exception.ImportConsentRequiredException;
import com.subscriptionmanager.importing.service.ImportService;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportAndConsentContractTest {

    private ImportService importService;
    private ImportConsentService importConsentService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        importService = Mockito.mock(ImportService.class);
        importConsentService = Mockito.mock(ImportConsentService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        ImportController importController = new ImportController(importService, currentUserService);
        ImportConsentController consentController = new ImportConsentController(importConsentService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(importController, consentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void importStartContract() throws Exception {
        String body = """
                {
                  "provider": "GMAIL",
                  "messages": [
                    {
                      "externalId": "gmail-msg-001",
                      "from": "billing@netflix.com",
                      "subject": "Netflix renewal notice",
                      "body": "Your Netflix renewal is scheduled for 2026-03-20. Amount: 9.99 USD",
                      "receivedAt": "2026-03-08T10:00:00Z"
                    }
                  ]
                }
                """;

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.start(eq(1L), any()))
                .thenReturn(new ImportResultResponse(
                        42L,
                        "GMAIL",
                        "COMPLETED_WITH_ERRORS",
                        1,
                        1,
                        0,
                        0,
                        OffsetDateTime.parse("2026-03-08T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-08T10:00:01Z"),
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(42))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").value(0))
                .andExpect(jsonPath("$.errorItems").isArray());
    }

    @Test
    void importStartEnabledButNotImplementedProviderContract() throws Exception {
        String body = """
                {
                  "provider": "YANDEX",
                  "messages": [
                    {
                      "externalId": "yandex-msg-not-impl-001",
                      "from": "billing@yandex.ru",
                      "subject": "Yandex Plus renewal",
                      "body": "Your renewal is scheduled for 2026-03-20. Amount: 299.00 RUB",
                      "receivedAt": "2026-03-08T10:00:00Z"
                    }
                  ]
                }
                """;

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.start(eq(1L), any()))
                .thenThrow(new ApiException("provider YANDEX is enabled but import flow is not implemented yet"));

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider YANDEX is enabled but import flow is not implemented yet"));
    }

    @Test
    void importStartLegacyErrorForNonGmailWhenFlagsAreOffContract() throws Exception {
        String body = """
                {
                  "provider": "YANDEX",
                  "messages": [
                    {
                      "externalId": "yandex-msg-off-001",
                      "from": "billing@yandex.ru",
                      "subject": "Yandex Plus renewal",
                      "body": "Your renewal is scheduled for 2026-03-20. Amount: 299.00 RUB",
                      "receivedAt": "2026-03-08T10:00:00Z"
                    }
                  ]
                }
                """;

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.start(eq(1L), any()))
                .thenThrow(new ApiException("only GMAIL provider is supported in MVP"));

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only GMAIL provider is supported in MVP"));
    }

    @Test
    void importHistoryContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.history(1L))
                .thenReturn(List.of(new ImportHistoryItemResponse(
                        42L,
                        "GMAIL",
                        "COMPLETED_WITH_ERRORS",
                        OffsetDateTime.parse("2026-03-08T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-08T10:00:01Z")
                )));

        mockMvc.perform(get("/api/v1/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].provider").value("GMAIL"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$[0].startedAt").exists())
                .andExpect(jsonPath("$[0].finishedAt").exists());
    }

    @Test
    void importDetailsContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.getById(1L, 42L))
                .thenReturn(new ImportResultResponse(
                        42L,
                        "GMAIL",
                        "COMPLETED_WITH_ERRORS",
                        3,
                        1,
                        1,
                        1,
                        OffsetDateTime.parse("2026-03-08T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-08T10:00:01Z"),
                        List.of(new ImportErrorItemResponse(
                                "gmail-msg-003",
                                "parser could not extract next billing date (expected YYYY-MM-DD)"
                        )),
                        List.of(new ImportItemResultResponse(
                                "gmail-msg-003",
                                "PARSE_ERROR",
                                "parser could not extract next billing date (expected YYYY-MM-DD)",
                                "netflix.com",
                                "Netflix",
                                null,
                                null,
                                null,
                                null,
                                "Entertainment",
                                OffsetDateTime.parse("2026-03-08T10:00:00Z")
                        ))
                ));

        mockMvc.perform(get("/api/v1/imports/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(42))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$.processed").value(3))
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors").value(1))
                .andExpect(jsonPath("$.errorItems[0].externalId").value("gmail-msg-003"))
                .andExpect(jsonPath("$.errorItems[0].reason")
                        .value("parser could not extract next billing date (expected YYYY-MM-DD)"));
    }

    @Test
    void importConsentRequiredErrorShapeContract() throws Exception {
        String body = """
                {
                  "provider": "GMAIL",
                  "messages": [
                    {
                      "externalId": "gmail-msg-no-consent-001",
                      "from": "billing@netflix.com",
                      "subject": "Netflix renewal notice",
                      "body": "Your Netflix renewal is scheduled for 2026-03-20. Amount: 9.99 USD",
                      "receivedAt": "2026-03-08T10:00:00Z"
                    }
                  ]
                }
                """;

        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importService.start(eq(1L), any()))
                .thenThrow(new ImportConsentRequiredException(ImportProvider.GMAIL));

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IMPORT_CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.message").value("active consent is required before import for provider GMAIL"));
    }

    @Test
    void importValidationErrorShapeContract() throws Exception {
        String body = """
                {
                  "messages": []
                }
                """;

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.provider").value("provider is required"))
                .andExpect(jsonPath("$.errors.messages").value("messages must contain at least one message"));
    }

    @Test
    void consentStatusGrantRevokeContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importConsentService.status(1L, "GMAIL"))
                .thenReturn(new ImportConsentStatusResponse(
                        "GMAIL",
                        "NOT_GRANTED",
                        "MAIL_IMPORT_READ",
                        null,
                        null,
                        "NOT_CONNECTED"
                ));
        when(importConsentService.grant(1L, "GMAIL"))
                .thenReturn(new ImportConsentStatusResponse(
                        "GMAIL",
                        "GRANTED",
                        "MAIL_IMPORT_READ",
                        OffsetDateTime.parse("2026-03-09T10:00:00Z"),
                        null,
                        "NOT_CONNECTED"
                ));
        when(importConsentService.revoke(1L, "GMAIL"))
                .thenReturn(new ImportConsentStatusResponse(
                        "GMAIL",
                        "REVOKED",
                        "MAIL_IMPORT_READ",
                        OffsetDateTime.parse("2026-03-09T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-09T11:00:00Z"),
                        "REVOKED"
                ));

        mockMvc.perform(get("/api/v1/consents/imports/GMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("NOT_GRANTED"))
                .andExpect(jsonPath("$.scope").value("MAIL_IMPORT_READ"))
                .andExpect(jsonPath("$.integrationStatus").value("NOT_CONNECTED"));

        mockMvc.perform(post("/api/v1/consents/imports/GMAIL/grant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("GRANTED"))
                .andExpect(jsonPath("$.scope").value("MAIL_IMPORT_READ"))
                .andExpect(jsonPath("$.integrationStatus").value("NOT_CONNECTED"));

        mockMvc.perform(post("/api/v1/consents/imports/GMAIL/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.scope").value("MAIL_IMPORT_READ"))
                .andExpect(jsonPath("$.integrationStatus").value("REVOKED"));
    }

    @Test
    void consentLegacyErrorsForNonGmailWhenFlagsAreOffContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importConsentService.status(1L, "YANDEX"))
                .thenThrow(new ApiException("provider must be one of: GMAIL"));
        when(importConsentService.grant(1L, "YANDEX"))
                .thenThrow(new ApiException("provider must be one of: GMAIL"));
        when(importConsentService.revoke(1L, "YANDEX"))
                .thenThrow(new ApiException("provider must be one of: GMAIL"));

        mockMvc.perform(get("/api/v1/consents/imports/YANDEX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider must be one of: GMAIL"));

        mockMvc.perform(post("/api/v1/consents/imports/YANDEX/grant"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider must be one of: GMAIL"));

        mockMvc.perform(post("/api/v1/consents/imports/YANDEX/revoke"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("provider must be one of: GMAIL"));
    }

    @Test
    void consentEndpointsContractForEnabledProviderScaffold() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(importConsentService.status(1L, "YANDEX"))
                .thenReturn(new ImportConsentStatusResponse(
                        "YANDEX",
                        "NOT_GRANTED",
                        "MAIL_IMPORT_READ",
                        null,
                        null,
                        "NOT_CONNECTED"
                ));
        when(importConsentService.grant(1L, "YANDEX"))
                .thenReturn(new ImportConsentStatusResponse(
                        "YANDEX",
                        "GRANTED",
                        "MAIL_IMPORT_READ",
                        OffsetDateTime.parse("2026-03-10T10:00:00Z"),
                        null,
                        "NOT_CONNECTED"
                ));
        when(importConsentService.revoke(1L, "YANDEX"))
                .thenReturn(new ImportConsentStatusResponse(
                        "YANDEX",
                        "REVOKED",
                        "MAIL_IMPORT_READ",
                        OffsetDateTime.parse("2026-03-10T10:00:00Z"),
                        OffsetDateTime.parse("2026-03-10T11:00:00Z"),
                        "REVOKED"
                ));

        mockMvc.perform(get("/api/v1/consents/imports/YANDEX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("YANDEX"))
                .andExpect(jsonPath("$.status").value("NOT_GRANTED"))
                .andExpect(jsonPath("$.scope").value("MAIL_IMPORT_READ"))
                .andExpect(jsonPath("$.integrationStatus").value("NOT_CONNECTED"));

        mockMvc.perform(post("/api/v1/consents/imports/YANDEX/grant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("YANDEX"))
                .andExpect(jsonPath("$.status").value("GRANTED"))
                .andExpect(jsonPath("$.scope").value("MAIL_IMPORT_READ"))
                .andExpect(jsonPath("$.integrationStatus").value("NOT_CONNECTED"));

        mockMvc.perform(post("/api/v1/consents/imports/YANDEX/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("YANDEX"))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.scope").value("MAIL_IMPORT_READ"))
                .andExpect(jsonPath("$.integrationStatus").value("REVOKED"));
    }
}
