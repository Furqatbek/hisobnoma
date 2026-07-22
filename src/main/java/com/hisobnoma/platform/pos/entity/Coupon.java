package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Coupon entity.
 * Represents a redeemable coupon code linked to a promotion.
 */
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupons_tenant", columnList = "tenant_id"),
    @Index(name = "idx_coupons_code", columnList = "code"),
    @Index(name = "idx_coupons_promotion", columnList = "promotion_id"),
    @Index(name = "idx_coupons_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Coupon extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(length = 200)
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Maximum times this coupon can be used
     */
    @Column(name = "max_uses")
    private Integer maxUses;

    /**
     * Current usage count
     */
    @Column(name = "current_uses")
    @Builder.Default
    private Integer currentUses = 0;

    /**
     * Maximum uses per customer
     */
    @Column(name = "max_uses_per_customer")
    @Builder.Default
    private Integer maxUsesPerCustomer = 1;

    /**
     * Specific customer ID (null = available to all)
     */
    @Column(name = "customer_id")
    private Long customerId;

    /**
     * Specific customer email (for guest checkouts)
     */
    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    /** Binds this coupon to ONE online-shop (web) customer; null = not web-customer-bound. */
    @Column(name = "web_customer_id")
    private Long webCustomerId;

    /**
     * First used at timestamp
     */
    @Column(name = "first_used_at")
    private Instant firstUsedAt;

    /**
     * Last used at timestamp
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL)
    @Builder.Default
    private List<CouponRedemption> redemptions = new ArrayList<>();

    public boolean isValid(LocalDate date) {
        if (status != CouponStatus.ACTIVE) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) {
            this.status = CouponStatus.EXPIRED;
            return false;
        }
        if (maxUses != null && currentUses >= maxUses) {
            this.status = CouponStatus.DEPLETED;
            return false;
        }
        return true;
    }

    public boolean canBeUsedByCustomer(Long customerId, int customerUsageCount) {
        // Check if coupon is customer-specific
        if (this.customerId != null && !this.customerId.equals(customerId)) {
            return false;
        }
        // Check per-customer usage limit
        if (maxUsesPerCustomer != null && customerUsageCount >= maxUsesPerCustomer) {
            return false;
        }
        return true;
    }

    public void recordUsage() {
        this.currentUses = (this.currentUses == null ? 0 : this.currentUses) + 1;
        this.lastUsedAt = Instant.now();
        if (this.firstUsedAt == null) {
            this.firstUsedAt = this.lastUsedAt;
        }
        if (maxUses != null && currentUses >= maxUses) {
            this.status = CouponStatus.DEPLETED;
        }
    }

    /**
     * Releases one recorded usage (e.g. a confirmed online order was cancelled).
     * Re-activates a coupon that was depleted only by the released usage.
     */
    public void releaseUsage() {
        this.currentUses = Math.max(0, (this.currentUses == null ? 0 : this.currentUses) - 1);
        if (status == CouponStatus.DEPLETED && (maxUses == null || currentUses < maxUses)) {
            this.status = CouponStatus.ACTIVE;
        }
    }
}
