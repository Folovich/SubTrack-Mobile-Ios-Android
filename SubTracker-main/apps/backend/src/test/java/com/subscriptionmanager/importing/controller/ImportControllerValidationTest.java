package com.subscriptionmanager.importing.controller;

import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.importing.exception.ImportConsentRequiredException;
import com.subscriptionmanager.importing.service.ImportService;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportControllerValidationTest {

    private ImportService importService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        importService = Mockito.mock(ImportService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        ImportController controller = new ImportController(importService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void startReturnsValidationErrorsForMissingProviderAndEmptyMessages() throws Exception {
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
    void startReturnsValidationErrorsForBrokenMessageFields() throws Exception {
        String body = """
                {
                  "provider": "GMAIL",
                  "messages": [
                    {
                      "externalId": "bad id with spaces",
                      "from": "not-an-email",
                      "subject": "",
                      "body": "",
                      "receivedAt": "2099-01-01T00:00:00Z"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors['messages[0].externalId']")
                        .value("externalId may contain only letters, digits, dot, underscore, colon and hyphen"))
                .andExpect(jsonPath("$.errors['messages[0].from']")
                        .value("from must be a valid email address"))
                .andExpect(jsonPath("$.errors['messages[0].subject']").value("subject is required"))
                .andExpect(jsonPath("$.errors['messages[0].body']").value("body is required"))
                .andExpect(jsonPath("$.errors['messages[0].receivedAt']")
                        .value("receivedAt must be in the past or present"));
    }

    @Test
    void startReturnsBadRequestForUnsupportedProvider() throws Exception {
        String body = """
                {
                  "provider": "YANDEX",
                  "messages": [
                    {
                      "externalId": "gmail-msg-unsupported-001",
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
                .thenThrow(new ApiException("only GMAIL provider is supported in MVP"));

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only GMAIL provider is supported in MVP"));
    }

    @Test
    void startReturnsControlledErrorForEnabledButNotImplementedProvider() throws Exception {
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
    void startReturnsForbiddenWhenConsentMissing() throws Exception {
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
                .thenThrow(new ImportConsentRequiredException(com.subscriptionmanager.common.enums.ImportProvider.GMAIL));

        mockMvc.perform(post("/api/v1/imports/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IMPORT_CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.provider").value("GMAIL"))
                .andExpect(jsonPath("$.message").value("active consent is required before import for provider GMAIL"));
    }
}
