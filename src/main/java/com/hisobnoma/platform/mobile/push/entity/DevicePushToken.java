package com.hisobnoma.platform.mobile.push.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * An APNs (Apple Push) device token for the admin mobile app. Keyed by the opaque device
 * {@code token} (unique), which is what a re-registration upserts on. Distinct from the FCM
 * {@code device_tokens} table used by the customer app — this subsystem talks to Apple directly.
 */
@Entity
@Table(name = "device_push_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_push_tokens_token", columnNames = {"tenant_id", "token"}),
        indexes = {
                @Index(name = "idx_device_push_tokens_tenant", columnList = "tenant_id"),
                @Index(name = "idx_device_push_tokens_user", columnList = "user_id")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DevicePushToken extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Opaque APNs device token (unique per tenant). */
    @Column(nullable = false, length = 512)
    private String token;

    /** "ios" (later "android"). */
    @Column(nullable = false, length = 20)
    private String platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PushEnvironment environment = PushEnvironment.PRODUCTION;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
