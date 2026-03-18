package com.subscriptionmanager.subscription.support;

import com.subscriptionmanager.subscription.dto.SupportEmailDraftResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailEventRequest;

public interface SupportEmailDraftService {
    SupportEmailDraftResponse getDraft(Long userId, Long subscriptionId, SupportEmailAction action);

    void trackEvent(Long userId, Long subscriptionId, SupportEmailEventRequest request);
}
