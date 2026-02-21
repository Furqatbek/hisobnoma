package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Product-specific alternate unit of measure with conversion factor to base UOM.
 * Example: Product "Sand" (base=TONNA) can have alternate UOM "SAMOSVOL" with factor 12.0
 * meaning 1 samosvol = 12 tonna.
 */
@Entity
@Table(name = "product_uoms", indexes = {
    @Index(name = "idx_product_uoms_product", columnList = "product_id"),
    @Index(name = "idx_product_uoms_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductUom extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UnitOfMeasure uom;

    /**
     * How many base units equal 1 of this alternate unit.
     * E.g. if base is TONNA and this UOM is SAMOSVOL, factor=12.0 means 1 samosvol = 12 tonna.
     */
    @Column(name = "conversion_factor", precision = 18, scale = 6, nullable = false)
    private BigDecimal conversionFactor;

    /**
     * Optional selling price per this alternate unit.
     * If null, the price is derived from product.sellingPrice * conversionFactor.
     */
    @Column(name = "selling_price", precision = 18, scale = 4)
    private BigDecimal sellingPrice;

    @Column(name = "is_default_sale", nullable = false)
    @Builder.Default
    private boolean defaultSale = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Convert a quantity in this alternate UOM to the product's base UOM.
     */
    public BigDecimal toBaseQuantity(BigDecimal alternateQuantity) {
        return alternateQuantity.multiply(conversionFactor);
    }

    /**
     * Convert a quantity in the product's base UOM to this alternate UOM.
     */
    public BigDecimal fromBaseQuantity(BigDecimal baseQuantity) {
        return baseQuantity.divide(conversionFactor, 6, RoundingMode.HALF_UP);
    }

    /**
     * Get effective selling price for this alternate UOM.
     * Falls back to product.sellingPrice * conversionFactor if no override.
     */
    public BigDecimal getEffectiveSellingPrice() {
        if (sellingPrice != null) {
            return sellingPrice;
        }
        if (product != null && product.getSellingPrice() != null) {
            return product.getSellingPrice().multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
