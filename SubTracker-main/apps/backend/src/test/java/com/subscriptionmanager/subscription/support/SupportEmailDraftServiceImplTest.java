package com.subscriptionmanager.subscription.support;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.SubscriptionNotFoundException;
import com.subscriptionmanager.exception.SubscriptionOwnershipException;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.SupportEmailEventRepository;
import com.subscriptionmanager.subscription.dto.SupportEmailDraftResponse;
import com.subscriptionmanager.subscription.dto.SupportEmailEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportEmailDraftServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SupportEmailEventRepository supportEmailEventRepository;

    private SupportEmailDraftServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SupportEmailDraftServiceImpl(
                subscriptionRepository,
                supportEmailEventRepository,
                new SupportEmailTemplateRegistry()
        );
    }

    @Test
    void getDraftBuildsMailtoAndReplacesPlaceholders() {
        Subscription subscription = sampleSubscription(1L, "Netflix");
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));

        SupportEmailDraftResponse response = service.getDraft(1L, 10L, SupportEmailAction.CANCEL);

        assertEquals(10L, response.subscriptionId());
        assertEquals(SupportEmailAction.CANCEL, response.action());
        assertEquals("GMAIL", response.provider());
        assertEquals("support@netflix.com", response.draft().to());
        assertTrue(response.draft().subject().contains("Netflix"));
        assertTrue(response.draft().body().contains("alice@example.com"));
        assertTrue(response.draft().body().contains("12.99 USD"));
        assertTrue(response.draft().mailtoUrl().startsWith("mailto:support@netflix.com?subject="));
        assertTrue(response.draft().mailtoUrl().contains("&body="));
        assertTrue(response.draft().plainTextForCopy().contains("Subject:"));
    }

    @Test
    void getDraftSanitizesHeaderInjectionInSubjectAndBodyPlaceholders() {
        Subscription subscription = sampleSubscription(1L, "Bad\r\nBcc:attacker@example.com");
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));

        SupportEmailDraftResponse response = service.getDraft(1L, 10L, SupportEmailAction.CANCEL);

        assertTrue(!response.draft().subject().contains("\n"));
        assertTrue(!response.draft().subject().contains("\r"));
        assertTrue(response.draft().body().contains("Bad Bcc:attacker@example.com"));
        assertTrue(response.draft().mailtoUrl().contains("%20"));
    }

    @Test
    void getDraftThrowsForbiddenForForeignSubscription() {
        Subscription subscription = sampleSubscription(99L, "Netflix");
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));

        assertThrows(SubscriptionOwnershipException.class, () -> service.getDraft(1L, 10L, SupportEmailAction.CANCEL));
    }

    @Test
    void getDraftThrowsNotFoundWhenSubscriptionMissing() {
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class, () -> service.getDraft(1L, 10L, SupportEmailAction.CANCEL));
    }

    @Test
    void trackEventPersistsActionEventAndProvider() {
        Subscription subscription = sampleSubscription(1L, "Netflix");
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));

        service.trackEvent(
                1L,
                10L,
                new SupportEmailEventRequest(SupportEmailAction.PAUSE, SupportEmailEventType.TEXT_COPIED)
        );

        ArgumentCaptor<com.subscriptionmanager.entity.SupportEmailEvent> captor =
                ArgumentCaptor.forClass(com.subscriptionmanager.entity.SupportEmailEvent.class);
        verify(supportEmailEventRepository).save(captor.capture());
        com.subscriptionmanager.entity.SupportEmailEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals(SupportEmailAction.PAUSE, event.getAction());
        assertEquals(SupportEmailEventType.TEXT_COPIED, event.getEventType());
        assertEquals("GMAIL", event.getProvider());
    }

    private Subscription sampleSubscription(Long ownerId, String serviceName) {
        User user = new User();
        user.setId(ownerId);
        user.setEmail("alice@example.com");

        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setUser(user);
        subscription.setServiceName(serviceName);
        subscription.setAmount(BigDecimal.valueOf(12.99));
        subscription.setCurrency("USD");
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setNextBillingDate(LocalDate.of(2026, 3, 30));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSourceType(SourceType.MAIL_IMPORT);
        return subscription;
    }
}
