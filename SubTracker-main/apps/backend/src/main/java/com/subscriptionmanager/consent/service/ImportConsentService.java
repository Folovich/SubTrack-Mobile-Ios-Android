package com.subscriptionmanager.consent.service;

import com.subscriptionmanager.consent.dto.ImportConsentStatusResponse;

public interface ImportConsentService {
    ImportConsentStatusResponse grant(Long userId, String providerRaw);
    ImportConsentStatusResponse revoke(Long userId, String providerRaw);
    ImportConsentStatusResponse status(Long userId, String providerRaw);
}
