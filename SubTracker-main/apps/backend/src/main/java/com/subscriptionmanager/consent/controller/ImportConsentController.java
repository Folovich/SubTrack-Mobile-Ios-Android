package com.subscriptionmanager.consent.controller;

import com.subscriptionmanager.consent.dto.ImportConsentStatusResponse;
import com.subscriptionmanager.consent.service.ImportConsentService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consents/imports")
public class ImportConsentController {

    private final ImportConsentService importConsentService;
    private final CurrentUserService currentUserService;

    public ImportConsentController(ImportConsentService importConsentService, CurrentUserService currentUserService) {
        this.importConsentService = importConsentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{provider}")
    public ImportConsentStatusResponse status(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return importConsentService.status(userId, provider);
    }

    @PostMapping("/{provider}/grant")
    public ImportConsentStatusResponse grant(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return importConsentService.grant(userId, provider);
    }

    @PostMapping("/{provider}/revoke")
    public ImportConsentStatusResponse revoke(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return importConsentService.revoke(userId, provider);
    }
}
