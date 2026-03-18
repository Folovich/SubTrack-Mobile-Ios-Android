package com.subscriptionmanager.usage.service;

import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.UsageSignal;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UsageSignalRepository;
import com.subscriptionmanager.usage.dto.UsageSignalCreateRequest;
import com.subscriptionmanager.usage.dto.UsageSignalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageSignalServiceImplTest {

    @Mock
    private UsageSignalRepository usageSignalRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private UsageSignalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsageSignalServiceImpl(usageSignalRepository, subscriptionRepository);
    }

    @Test
    void createSavesSignalForOwnedSubscription() {
        Subscription subscription = ownedSubscription(123L, 1L);
        when(subscriptionRepository.findByIdAndUserId(123L, 1L)).thenReturn(Optional.of(subscription));
        when(usageSignalRepository.save(any(UsageSignal.class))).thenAnswer(invocation -> {
            UsageSignal signal = invocation.getArgument(0);
            signal.setId(10L);
            signal.setCreatedAt(OffsetDateTime.parse("2026-03-12T10:15:30Z"));
            return signal;
        });

        UsageSignalResponse response = service.create(
                1L,
                new UsageSignalCreateRequest(123L, " CONTENT_WATCHED ", " episode=3 ")
        );

        ArgumentCaptor<UsageSignal> captor = ArgumentCaptor.forClass(UsageSignal.class);
        verify(usageSignalRepository).save(captor.capture());
        UsageSignal saved = captor.getValue();

        assertEquals(1L, saved.getUser().getId());
        assertEquals(123L, saved.getSubscription().getId());
        assertEquals("CONTENT_WATCHED", saved.getSignalType());
        assertEquals("episode=3", saved.getValue());

        assertEquals(10L, response.id());
        assertEquals(123L, response.subscriptionId());
        assertEquals("CONTENT_WATCHED", response.signalType());
        assertEquals("episode=3", response.value());
        assertEquals("2026-03-12T10:15:30Z", response.createdAt());
    }

    @Test
    void createThrowsWhenSubscriptionNotFound() {
        when(subscriptionRepository.findByIdAndUserId(123L, 1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.create(1L, new UsageSignalCreateRequest(123L, "CONTENT_WATCHED", "episode=3"))
        );

        assertEquals("Subscription not found", exception.getMessage());
        verifyNoInteractions(usageSignalRepository);
    }

    @Test
    void listReturnsSignalsForCurrentUserWhenFilterMissing() {
        when(usageSignalRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(
                        usageSignal(11L, 123L, 1L, "CONTENT_WATCHED", "episode=4", "2026-03-12T10:16:00Z"),
                        usageSignal(10L, 123L, 1L, "CONTENT_WATCHED", "episode=3", "2026-03-12T10:15:30Z")
                ));

        List<UsageSignalResponse> response = service.list(1L, null);

        assertEquals(2, response.size());
        assertEquals(11L, response.get(0).id());
        assertEquals(10L, response.get(1).id());
    }

    @Test
    void listThrowsWhenFilterSubscriptionNotOwned() {
        when(subscriptionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.list(1L, 999L));

        assertEquals("Subscription not found", exception.getMessage());
    }

    @Test
    void listThrowsWhenFilterSubscriptionIdIsInvalid() {
        ApiException exception = assertThrows(ApiException.class, () -> service.list(1L, 0L));
        assertEquals("subscriptionId must be greater than 0", exception.getMessage());
    }

    @Test
    void listReturnsSignalsForOwnedSubscriptionFilter() {
        Subscription subscription = ownedSubscription(123L, 1L);
        when(subscriptionRepository.findByIdAndUserId(123L, 1L)).thenReturn(Optional.of(subscription));
        when(usageSignalRepository.findByUserIdAndSubscriptionIdOrderByCreatedAtDesc(1L, 123L))
                .thenReturn(List.of(
                        usageSignal(12L, 123L, 1L, "APP_OPENED", "session=morning", "2026-03-12T11:00:00Z")
                ));

        List<UsageSignalResponse> response = service.list(1L, 123L);

        assertEquals(1, response.size());
        assertEquals(12L, response.getFirst().id());
        assertEquals("APP_OPENED", response.getFirst().signalType());
    }

    private Subscription ownedSubscription(Long subscriptionId, Long userId) {
        User user = new User();
        user.setId(userId);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUser(user);
        return subscription;
    }

    private UsageSignal usageSignal(
            Long id,
            Long subscriptionId,
            Long userId,
            String signalType,
            String value,
            String createdAt
    ) {
        User user = new User();
        user.setId(userId);

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setUser(user);

        UsageSignal signal = new UsageSignal();
        signal.setId(id);
        signal.setUser(user);
        signal.setSubscription(subscription);
        signal.setSignalType(signalType);
        signal.setValue(value);
        signal.setCreatedAt(OffsetDateTime.parse(createdAt));
        return signal;
    }
}
