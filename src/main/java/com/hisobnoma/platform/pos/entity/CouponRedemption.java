package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * CouponRedemption entity.
 * Tracks each use of a coupon.
 */
@Entity
@Table(name = "coupon_redemptions", indexes = {
    @Index(name = "idx_coupon_redemptions_coupon", columnList = "coupon_id"),
    @Index(name = "idx_coupon_redemptions_customer", columnList = "customer_id"),
    @Index(name = "idx_coupon_redemptions_transaction", columnList = "transaction_id"),
    @Index(name = "idx_coupon_redemptions_redeemed_at", columnList = "redeemed_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CouponRedemption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private POSTransaction transaction;

    /**
     * Online order this redemption belongs to (null for POS redemptions).
     */
    @Column(name = "web_order_id")
    private Long webOrderId;

    /**
     * Order total before discount
     */
    @Column(name = "order_total", precision = 18, scale = 4)
    private BigDecimal orderTotal;

    /**
     * Discount amount applied
     */
    @Column(name = "discount_amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;

    @Column(name = "redeemed_by")
    private Long redeemedBy;

    /**
     * IP address of the redemption
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Was this redemption reversed/refunded?
     */
    @Column(name = "is_reversed")
    @Builder.Default
    private boolean reversed = false;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by")
    private Long reversedBy;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @Column(length = 500)
    private String notes;
}
