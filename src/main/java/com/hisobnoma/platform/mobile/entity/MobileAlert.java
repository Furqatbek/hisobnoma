package com.hisobnoma.platform.mobile.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Mobile alert entity for storing alerts and notifications.
 */
@Entity
@Table(name = "mobile_alerts", indexes = {
        @Index(name = "idx_mobile_alert_user", columnList = "user_id"),
        @Index(name = "idx_mobile_alert_tenant", columnList = "tenant_id"),
        @Index(name = "idx_mobile_alert_type", columnList = "alert_type"),
        @Index(name = "idx_mobile_alert_read", columnList = "is_read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MobileAlert extends TenantAwareEntity {

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private AlertPriority priority = AlertPriority.NORMAL;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sent_via_push", nullable = false)
    @Builder.Default
    private boolean sentViaPush = false;

    @Column(name = "push_sent_at")
    private Instant pushSentAt;

    public enum AlertType {
        LOW_STOCK,
        OUT_OF_STOCK,
        EXPIRING_INVENTORY,
        LARGE_TRANSACTION,
        PAYMENT_RECEIVED,
        PAYMENT_OVERDUE,
        ORDER_PLACED,
        ORDER_CANCELLED,
        SYSTEM_ALERT,
        DAILY_SUMMARY,
        WEEKLY_SUMMARY,
        APPROVAL_REQUIRED,
        CUSTOM
    }

    public enum AlertPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
