package com.subscriptionmanager.entity;

import com.subscriptionmanager.common.enums.NotificationType;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_rules")
public class NotificationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private Integer daysBefore;

    @Column(nullable = false)
    private Boolean enabled;
}
