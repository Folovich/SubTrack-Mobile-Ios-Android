package com.subscriptionmanager.importing.service;

import com.subscriptionmanager.importing.dto.ImportHistoryItemResponse;
import com.subscriptionmanager.importing.dto.ImportResultResponse;
import com.subscriptionmanager.importing.dto.ImportStartRequest;

import java.util.List;

public interface ImportService {
    ImportResultResponse start(Long userId, ImportStartRequest request);
    ImportResultResponse syncMailbox(Long userId, String providerRaw);
    List<ImportHistoryItemResponse> history(Long userId);
    ImportResultResponse getById(Long userId, Long jobId);
}
