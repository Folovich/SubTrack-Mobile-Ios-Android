package com.subscriptionmanager.repository;

import com.subscriptionmanager.common.enums.BillingPeriod;
import com.subscriptionmanager.common.enums.SubscriptionStatus;
import com.subscriptionmanager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserIdOrderByNextBillingDateAsc(Long userId);

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByStatus(SubscriptionStatus status);

    boolean existsByUserIdAndServiceNameIgnoreCaseAndAmountAndCurrencyAndBillingPeriodAndNextBillingDateAndStatus(
            Long userId,
            String serviceName,
            BigDecimal amount,
            String currency,
            BillingPeriod billingPeriod,
            LocalDate nextBillingDate,
            SubscriptionStatus status
    );
    List<Subscription> findByUserIdAndStatusAndNextBillingDateBetweenOrderByNextBillingDateAsc(
            Long userId,
            SubscriptionStatus status,
            LocalDate from,
            LocalDate to
    );

    boolean existsByUserIdAndImportFingerprintAndStatus(Long userId, String importFingerprint, SubscriptionStatus status);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);
}
