package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * One-time SMS login code. Only the salted SHA-256 hash is stored.
 */
@Entity
@Table(name = "web_otp_codes", indexes = {
        @Index(name = "idx_web_otp_phone", columnList = "tenant_id, phone, created_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebOtpCode extends TenantAwareEntity {

    /**
     * Digits-only normalized phone.
     */
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
