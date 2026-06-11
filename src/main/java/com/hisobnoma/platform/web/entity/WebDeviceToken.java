package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "web_device_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_web_device_tokens_token_tenant",
                columnNames = {"tenant_id", "token"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebDeviceToken extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "web_customer_id", nullable = false)
    private Long webCustomerId;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
