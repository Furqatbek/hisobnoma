package com.hisobnoma.platform.mobile.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Alert preference entity for user notification settings.
 */
@Entity
@Table(name = "alert_preferences", indexes = {
        @Index(name = "idx_alert_pref_user", columnList = "user_id"),
        @Index(name = "idx_alert_pref_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_alert_pref_user_type", columnNames = {"user_id", "alert_type", "tenant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class AlertPreference extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private MobileAlert.AlertType alertType;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private boolean pushEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = false;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = false;

    @Column(name = "telegram_enabled", nullable = false)
    @Builder.Default
    private boolean telegramEnabled = false;

    @Column(name = "threshold_value")
    private Integer thresholdValue;

    @Column(name = "quiet_hours_start")
    private Integer quietHoursStart;

    @Column(name = "quiet_hours_end")
    private Integer quietHoursEnd;
}
