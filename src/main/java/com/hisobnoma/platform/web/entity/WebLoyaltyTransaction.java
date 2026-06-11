package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "web_loyalty_transactions",
        indexes = {
                @Index(name = "idx_wlt_tenant_customer", columnList = "tenant_id, web_customer_id"),
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebLoyaltyTransaction extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "web_customer_id", nullable = false)
    private Long webCustomerId;

    @Column(name = "web_order_id")
    private Long webOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WebLoyaltyTransactionType type;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by")
    private Long createdBy;
}
