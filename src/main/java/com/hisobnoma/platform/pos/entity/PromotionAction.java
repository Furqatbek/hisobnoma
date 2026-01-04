package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * PromotionAction entity.
 * Defines what happens when a promotion is applied.
 */
@Entity
@Table(name = "promotion_actions", indexes = {
    @Index(name = "idx_promotion_actions_promotion", columnList = "promotion_id"),
    @Index(name = "idx_promotion_actions_type", columnList = "action_type")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PromotionAction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    /**
     * Type of action (PERCENTAGE_OFF, FIXED_AMOUNT_OFF, FREE_ITEM, SET_PRICE)
     */
    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    /**
     * Discount percentage (for PERCENTAGE_OFF)
     */
    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    /**
     * Discount amount (for FIXED_AMOUNT_OFF)
     */
    @Column(name = "discount_amount", precision = 18, scale = 4)
    private BigDecimal discountAmount;

    /**
     * Fixed price (for SET_PRICE)
     */
    @Column(name = "set_price", precision = 18, scale = 4)
    private BigDecimal setPrice;

    /**
     * Maximum discount allowed
     */
    @Column(name = "max_discount", precision = 18, scale = 4)
    private BigDecimal maxDiscount;

    /**
     * Product ID to give free (for FREE_ITEM)
     */
    @Column(name = "free_product_id")
    private Long freeProductId;

    /**
     * Quantity of free items
     */
    @Column(name = "free_quantity")
    @Builder.Default
    private Integer freeQuantity = 1;

    /**
     * Target product IDs for line-item discounts (comma-separated)
     */
    @Column(name = "target_product_ids", length = 2000)
    private String targetProductIds;

    /**
     * Target category IDs (comma-separated)
     */
    @Column(name = "target_category_ids", length = 1000)
    private String targetCategoryIds;

    /**
     * Apply to cheapest/most expensive items in qualifying set
     */
    @Column(name = "apply_to", length = 20)
    private String applyTo; // CHEAPEST, MOST_EXPENSIVE, ALL

    /**
     * Number of items to apply discount to
     */
    @Column(name = "apply_count")
    private Integer applyCount;

    /**
     * Order of application when multiple actions exist
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 500)
    private String notes;
}
