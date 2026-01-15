package com.hisobnoma.platform.mobile.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Device token entity for push notifications.
 * Stores FCM (Firebase Cloud Messaging) tokens for mobile devices.
 */
@Entity
@Table(name = "device_tokens", indexes = {
        @Index(name = "idx_device_token_user", columnList = "user_id"),
        @Index(name = "idx_device_token_tenant", columnList = "tenant_id"),
        @Index(name = "idx_device_token_platform", columnList = "platform")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum Platform {
        IOS,
        ANDROID,
        WEB
    }
}
