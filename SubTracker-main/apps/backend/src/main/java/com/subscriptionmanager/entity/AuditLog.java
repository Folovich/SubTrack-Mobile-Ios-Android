package com.subscriptionmanager.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
