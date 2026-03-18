package com.subscriptionmanager.usage.service;

import com.subscriptionmanager.usage.dto.UsageSignalCreateRequest;
import com.subscriptionmanager.usage.dto.UsageSignalResponse;

import java.util.List;

public interface UsageSignalService {
    UsageSignalResponse create(Long userId, UsageSignalCreateRequest request);

    List<UsageSignalResponse> list(Long userId, Long subscriptionId);
}
