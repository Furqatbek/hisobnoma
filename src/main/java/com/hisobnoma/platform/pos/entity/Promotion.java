package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Promotion entity.
 * Represents a promotional offer with conditions and actions.
 */
@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promotions_tenant", columnList = "tenant_id"),
    @Index(name = "idx_promotions_code", columnList = "code"),
    @Index(name = "idx_promotions_type", columnList = "type"),
    @Index(name = "idx_promotions_active", columnList = "is_active"),
    @Index(name = "idx_promotions_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Promotion extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromotionScope scope = PromotionScope.ORDER;

    /**
     * Sales channel: POS terminal only (default), online shop only, or both.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PromotionChannel channel = PromotionChannel.POS;

    /**
     * Priority for stacking (higher = applied first)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /**
     * Discount value (percentage or fixed amount based on type)
     */
    @Column(name = "discount_value", precision = 18, scale = 4)
    private BigDecimal discountValue;

    /**
     * Maximum discount amount (for percentage discounts)
     */
    @Column(name = "max_discount_amount", precision = 18, scale = 4)
    private BigDecimal maxDiscountAmount;

    /**
     * For BUY_X_GET_Y: quantity to buy
     */
    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    /**
     * For BUY_X_GET_Y: quantity to get free/discounted
     */
    @Column(name = "get_quantity")
    private Integer getQuantity;

    /**
     * For BUY_X_GET_Y: discount on the "get" items (100 = free)
     */
    @Column(name = "get_discount_percent", precision = 5, scale = 2)
    private BigDecimal getDiscountPercent;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Time-based restrictions
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * Days of week (comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN)
     */
    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    /**
     * Can this promotion be combined with others?
     */
    @Column(name = "is_stackable")
    @Builder.Default
    private boolean stackable = false;

    /**
     * Require a coupon code to activate
     */
    @Column(name = "requires_coupon")
    @Builder.Default
    private boolean requiresCoupon = false;

    /**
     * Maximum number of times this promotion can be used in total
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
    private Integer maxUsesPerCustomer;

    /**
     * Minimum order amount to qualify
     */
    @Column(name = "min_order_amount", precision = 18, scale = 4)
    private BigDecimal minOrderAmount;

    /**
     * Location-specific promotion (null = all locations)
     */
    @Column(name = "location_id")
    private Long locationId;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionAction> actions = new ArrayList<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Coupon> coupons = new ArrayList<>();

    public boolean isEffective(LocalDate date, LocalTime time) {
        if (!active) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;

        // Check time restrictions
        if (startTime != null && endTime != null && time != null) {
            if (time.isBefore(startTime) || time.isAfter(endTime)) return false;
        }

        // Check day of week
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            String today = date.getDayOfWeek().name().substring(0, 3);
            if (!daysOfWeek.toUpperCase().contains(today)) return false;
        }

        // Check usage limits
        if (maxUses != null && currentUses >= maxUses) return false;

        return true;
    }

    public void incrementUsage() {
        this.currentUses = (this.currentUses == null ? 0 : this.currentUses) + 1;
    }

    /**
     * Releases one recorded usage (e.g. when a confirmed online order is cancelled).
     */
    public void decrementUsage() {
        this.currentUses = Math.max(0, (this.currentUses == null ? 0 : this.currentUses) - 1);
    }

    public void addCondition(PromotionCondition condition) {
        conditions.add(condition);
        condition.setPromotion(this);
    }

    public void addAction(PromotionAction action) {
        actions.add(action);
        action.setPromotion(this);
    }
}
