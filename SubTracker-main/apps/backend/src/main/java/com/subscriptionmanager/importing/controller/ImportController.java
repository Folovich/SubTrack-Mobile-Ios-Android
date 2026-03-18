package com.subscriptionmanager.importing.controller;

import com.subscriptionmanager.importing.dto.ImportHistoryItemResponse;
import com.subscriptionmanager.importing.dto.ImportResultResponse;
import com.subscriptionmanager.importing.dto.ImportStartRequest;
import com.subscriptionmanager.importing.service.ImportService;
import jakarta.validation.Valid;
import com.subscriptionmanager.security.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService importService;
    private final CurrentUserService currentUserService;

    public ImportController(ImportService importService, CurrentUserService currentUserService) {
        this.importService = importService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/start")
    public ImportResultResponse start(@Valid @RequestBody ImportStartRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        return importService.start(userId, request);
    }

    @PostMapping("/{provider}/sync")
    public ImportResultResponse sync(@PathVariable String provider) {
        Long userId = currentUserService.requireCurrentUserId();
        return importService.syncMailbox(userId, provider);
    }

    @GetMapping
    public List<ImportHistoryItemResponse> history() {
        Long userId = currentUserService.requireCurrentUserId();
        return importService.history(userId);
    }

    @GetMapping("/{id}")
    public ImportResultResponse details(@PathVariable Long id) {
        Long userId = currentUserService.requireCurrentUserId();
        return importService.getById(userId, id);
    }
}
