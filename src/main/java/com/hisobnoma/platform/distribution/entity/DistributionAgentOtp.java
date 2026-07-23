package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * One-time SMS login code for a distribution agent's mobile app.
 * Dedicated to the distribution module (not shared with web-customer OTP) so
 * an agent phone and a shop-customer phone never share codes or daily limits.
 * Only the salted SHA-256 hash is stored.
 */
@Entity
@Table(name = "distribution_agent_otps", indexes = {
        @Index(name = "idx_dist_agent_otp_phone", columnList = "tenant_id, phone, created_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionAgentOtp extends TenantAwareEntity {

    /** Digits-only normalized phone. */
    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false, length = 32)
    private String salt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;
}
