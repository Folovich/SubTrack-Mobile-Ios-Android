package com.subscriptionmanager.usage.controller;

import com.subscriptionmanager.security.CurrentUserService;
import com.subscriptionmanager.usage.dto.UsageSignalCreateRequest;
import com.subscriptionmanager.usage.dto.UsageSignalResponse;
import com.subscriptionmanager.usage.service.UsageSignalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usage-signals")
public class UsageSignalController {

    private final UsageSignalService usageSignalService;
    private final CurrentUserService currentUserService;

    public UsageSignalController(UsageSignalService usageSignalService, CurrentUserService currentUserService) {
        this.usageSignalService = usageSignalService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public UsageSignalResponse create(@Valid @RequestBody UsageSignalCreateRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        return usageSignalService.create(userId, request);
    }

    @GetMapping
    public List<UsageSignalResponse> list(@RequestParam(required = false) Long subscriptionId) {
        Long userId = currentUserService.requireCurrentUserId();
        return usageSignalService.list(userId, subscriptionId);
    }
}
