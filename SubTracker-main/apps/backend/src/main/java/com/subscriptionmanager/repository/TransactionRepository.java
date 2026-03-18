package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdAndOperationDateBetween(Long userId, LocalDate from, LocalDate to);
}
