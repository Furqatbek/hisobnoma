package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PriceListItem entity.
 * Represents a specific price for a product/variant in a price list.
 */
@Entity
@Table(name = "price_list_items", indexes = {
    @Index(name = "idx_price_list_items_price_list", columnList = "price_list_id"),
    @Index(name = "idx_price_list_items_product", columnList = "product_id"),
    @Index(name = "idx_price_list_items_variant", columnList = "variant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PriceListItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    /**
     * Fixed price (takes precedence over markup)
     */
    @Column(precision = 18, scale = 4)
    private BigDecimal price;

    /**
     * Minimum price (floor price)
     */
    @Column(name = "min_price", precision = 18, scale = 4)
    private BigDecimal minPrice;

    /**
     * Maximum price (ceiling price)
     */
    @Column(name = "max_price", precision = 18, scale = 4)
    private BigDecimal maxPrice;

    /**
     * Markup/markdown percentage from product's base price
     */
    @Column(name = "markup_percent", precision = 5, scale = 2)
    private BigDecimal markupPercent;

    /**
     * Minimum quantity for this price to apply (for tiered pricing)
     */
    @Column(name = "min_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal minQuantity = BigDecimal.ONE;

    /**
     * Maximum quantity for this price tier
     */
    @Column(name = "max_quantity", precision = 18, scale = 4)
    private BigDecimal maxQuantity;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(length = 500)
    private String notes;

    /**
     * Calculate the effective price based on base price
     */
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        if (price != null) {
            return applyMinMax(price);
        }

        if (markupPercent != null) {
            BigDecimal markup = basePrice.multiply(markupPercent)
                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            return applyMinMax(basePrice.add(markup));
        }

        return applyMinMax(basePrice);
    }

    private BigDecimal applyMinMax(BigDecimal calculatedPrice) {
        if (minPrice != null && calculatedPrice.compareTo(minPrice) < 0) {
            return minPrice;
        }
        if (maxPrice != null && calculatedPrice.compareTo(maxPrice) > 0) {
            return maxPrice;
        }
        return calculatedPrice;
    }

    public boolean isEffective(LocalDate date) {
        if (!active) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return true;
    }

    public boolean appliesToQuantity(BigDecimal quantity) {
        if (minQuantity != null && quantity.compareTo(minQuantity) < 0) return false;
        if (maxQuantity != null && quantity.compareTo(maxQuantity) > 0) return false;
        return true;
    }
}
