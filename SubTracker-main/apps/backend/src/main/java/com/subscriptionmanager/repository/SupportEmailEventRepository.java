package com.subscriptionmanager.repository;

import com.subscriptionmanager.entity.SupportEmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportEmailEventRepository extends JpaRepository<SupportEmailEvent, Long> {
}
