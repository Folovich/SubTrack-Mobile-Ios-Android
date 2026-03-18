package com.subscriptionmanager.subscription.service;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SourceType;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Subscription;
import com.subscriptionmanager.entity.SubscriptionEvent;
import com.subscriptionmanager.entity.User;
import com.subscriptionmanager.exception.ApiException;
import com.subscriptionmanager.repository.CategoryRepository;
import com.subscriptionmanager.repository.SubscriptionEventRepository;
import com.subscriptionmanager.repository.SubscriptionRepository;
import com.subscriptionmanager.repository.UserRepository;
import com.subscriptionmanager.subscription.dto.SubscriptionRequest;
import com.subscriptionmanager.subscription.dto.UpcomingSubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionEventRepository subscriptionEventRepository;

    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionServiceImpl(
                subscriptionRepository,
                categoryRepository,
                userRepository,
                subscriptionEventRepository
        );
    }

    @Test
    void upcomingReturnsActiveSubscriptionsWithDaysUntilBilling() {
        LocalDate today = LocalDate.now();
        LocalDate nextBillingDate = today.plusDays(2);

        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setServiceName("Netflix");
        subscription.setAmount(BigDecimal.valueOf(9.99));
        subscription.setCurrency("USD");
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSourceType(SourceType.MANUAL);

        when(subscriptionRepository.findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
                1L,
                SubscriptionStatus.ACTIVE,
                today,
                today.plusDays(7)
        )).thenReturn(List.of(subscription));

        List<UpcomingSubscriptionResponse> response = service.upcoming(1L, 7);

        assertEquals(1, response.size());
        assertEquals("Netflix", response.getFirst().serviceName());
        assertEquals(2, response.getFirst().daysUntilBilling());
    }

    @Test
    void upcomingThrowsForInvalidDays() {
        assertThrows(ApiException.class, () -> service.upcoming(1L, 0));
    }

    @Test
    void updateCreatesPriceChangeEventWhenAmountChanged() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        Subscription existing = new Subscription();
        existing.setId(10L);
        existing.setUser(user);
        existing.setServiceName("Netflix");
        existing.setAmount(BigDecimal.valueOf(9.99));
        existing.setCurrency("USD");
        existing.setBillingPeriod(BillingPeriod.MONTHLY);
        existing.setNextBillingDate(LocalDate.now().plusDays(5));
        existing.setStatus(SubscriptionStatus.ACTIVE);
        existing.setSourceType(SourceType.MANUAL);

        SubscriptionRequest request = new SubscriptionRequest(
                "Netflix",
                null,
                BigDecimal.valueOf(12.99),
                "USD",
                BillingPeriod.MONTHLY,
                LocalDate.now().plusDays(5),
                SubscriptionStatus.ACTIVE
        );

        when(subscriptionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(1L, 10L, request);

        ArgumentCaptor<SubscriptionEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionEvent.class);
        verify(subscriptionEventRepository).save(eventCaptor.capture());
        SubscriptionEvent event = eventCaptor.getValue();
        assertEquals("PRICE_CHANGE", event.getEventType());
        assertEquals(BigDecimal.valueOf(9.99), event.getOldAmount());
        assertEquals(BigDecimal.valueOf(12.99), event.getNewAmount());
    }
}
