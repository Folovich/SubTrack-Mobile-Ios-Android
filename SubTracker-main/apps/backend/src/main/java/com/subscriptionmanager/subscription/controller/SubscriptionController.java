package com.subscriptionmanager.subscription.controller;

import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.subscription.dto.SubscriptionRequest;
import com.subscriptionmanager.subscription.dto.SubscriptionResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailDraftResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailEventRequest;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;
import com.subscriptionmanager.subscription.support.SupportEmailAction;
import com.subscriptionmanager.subscription.support.SupportEmailDraftService;
import com.subscriptionmanager.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;
    private final SupportEmailDraftService supportEmailDraftService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            CurrentUserService currentUserService,
            SupportEmailDraftService supportEmailDraftService
    ) {
        this.subscriptionService = subscriptionService;
        this.currentUserService = currentUserService;
        this.supportEmailDraftService = supportEmailDraftService;
    }

    @GetMapping
    public List<SubscriptionResponse> list() {
        Long userId = currentUserService.requireCurrentUserId();
        return subscriptionService.list(userId);
    }

    @GetMapping("/upcoming")
    public List<UpcomingSubscriptionResponse> upcoming(@RequestParam(defaultValue = "7") int days) {
        Long userId = currentUserService.requireCurrentUserId();
        return subscriptionService.upcoming(userId, days);
    }

    @GetMapping("/{id}")
    public SubscriptionResponse getById(@PathVariable Long id) {
        Long userId = currentUserService.requireCurrentUserId();
        return subscriptionService.getById(userId, id);
    }

    @PostMapping
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        return subscriptionService.create(userId, request);
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable Long id, @Valid @RequestBody SubscriptionRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        return subscriptionService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Long userId = currentUserService.requireCurrentUserId();
        subscriptionService.delete(userId, id);
    }

    @GetMapping("/{id}/support-email-draft")
    public SupportEmailDraftResponse supportEmailDraft(
            @PathVariable Long id,
            @RequestParam SupportEmailAction action
    ) {
        Long userId = currentUserService.requireCurrentUserId();
        return supportEmailDraftService.getDraft(userId, id, action);
    }

    @PostMapping("/{id}/support-email-events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supportEmailEvent(@PathVariable Long id, @Valid @RequestBody SupportEmailEventRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        supportEmailDraftService.trackEvent(userId, id, request);
    }
}
