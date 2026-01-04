package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * PromotionCondition entity.
 * Defines conditions that must be met for a promotion to apply.
 */
@Entity
@Table(name = "promotion_conditions", indexes = {
    @Index(name = "idx_promotion_conditions_promotion", columnList = "promotion_id"),
    @Index(name = "idx_promotion_conditions_type", columnList = "condition_type")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PromotionCondition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 30)
    private PromotionConditionType conditionType;

    /**
     * Operator for comparison (EQ, NE, GT, GTE, LT, LTE, IN, NOT_IN, BETWEEN)
     */
    @Column(length = 10)
    @Builder.Default
    private String operator = "GTE";

    /**
     * Value for comparison (numeric or string based on condition type)
     */
    @Column(name = "condition_value", length = 500)
    private String value;

    /**
     * Secondary value (for BETWEEN operator)
     */
    @Column(name = "condition_value2", length = 500)
    private String value2;

    /**
     * Numeric threshold (for MINIMUM_PURCHASE, MINIMUM_QUANTITY)
     */
    @Column(name = "threshold_amount", precision = 18, scale = 4)
    private BigDecimal thresholdAmount;

    /**
     * Product IDs (comma-separated) for SPECIFIC_PRODUCTS
     */
    @Column(name = "product_ids", length = 2000)
    private String productIds;

    /**
     * Category IDs (comma-separated) for CATEGORY
     */
    @Column(name = "category_ids", length = 1000)
    private String categoryIds;

    /**
     * Brand IDs (comma-separated) for BRAND
     */
    @Column(name = "brand_ids", length = 1000)
    private String brandIds;

    /**
     * Customer group names (comma-separated) for CUSTOMER_GROUP
     */
    @Column(name = "customer_groups", length = 500)
    private String customerGroups;

    /**
     * If true, all conditions must be met (AND). If false, any condition (OR).
     */
    @Column(name = "is_required")
    @Builder.Default
    private boolean required = true;

    @Column(length = 500)
    private String notes;
}
