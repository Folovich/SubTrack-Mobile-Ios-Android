package com.subscriptionmanager.integration.controller;

import com.subscriptionmanager.integration.dto.IntegrationConnectionResponse;
import com.subscriptionmanager.integration.dto.OAuthStartResponse;
import com.subscriptionmanager.integration.service.IntegrationService;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;
    private final CurrentUserService currentUserService;

    public IntegrationController(IntegrationService integrationService, CurrentUserService currentUserService) {
        this.integrationService = integrationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<IntegrationConnectionResponse> list() {
        Long userId = currentUserService.requireCurrentUserId();
        return integrationService.list(userId);
    }

    @GetMapping("/{provider}")
    public IntegrationConnectionResponse status(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return integrationService.status(userId, provider);
    }

    @PostMapping("/{provider}/oauth/start")
    public OAuthStartResponse startOAuth(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return integrationService.startOAuth(userId, provider);
    }

    @GetMapping("/{provider}/oauth/callback")
    public RedirectView callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        return new RedirectView(integrationService.handleOAuthCallback(provider, code, state, error));
    }

    @PostMapping("/{provider}/disconnect")
    public IntegrationConnectionResponse disconnect(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return integrationService.disconnect(userId, provider);
    }
}
