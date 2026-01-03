package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * ProductVariant entity for product variations (e.g., size, color combinations).
 * Each variant can have its own SKU, barcode, and pricing.
 */
@Entity
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_variants_product", columnList = "product_id"),
    @Index(name = "idx_variants_sku", columnList = "sku"),
    @Index(name = "idx_variants_barcode", columnList = "barcode"),
    @Index(name = "idx_variants_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductVariant extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(length = 50)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String option1Name;

    @Column(length = 100)
    private String option1Value;

    @Column(length = 100)
    private String option2Name;

    @Column(length = 100)
    private String option2Value;

    @Column(length = 100)
    private String option3Name;

    @Column(length = 100)
    private String option3Value;

    @Column(name = "cost_price", precision = 18, scale = 4)
    private BigDecimal costPrice;

    @Column(name = "selling_price", precision = 18, scale = 4)
    private BigDecimal sellingPrice;

    @Column(name = "price_difference", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal priceDifference = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public BigDecimal getEffectiveSellingPrice() {
        if (sellingPrice != null) {
            return sellingPrice;
        }
        if (product != null && product.getSellingPrice() != null) {
            return product.getSellingPrice().add(priceDifference != null ? priceDifference : BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getEffectiveCostPrice() {
        if (costPrice != null) {
            return costPrice;
        }
        return product != null ? product.getCostPrice() : BigDecimal.ZERO;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (product != null) {
            sb.append(product.getName());
        }
        if (option1Value != null) {
            sb.append(" - ").append(option1Value);
        }
        if (option2Value != null) {
            sb.append(" / ").append(option2Value);
        }
        if (option3Value != null) {
            sb.append(" / ").append(option3Value);
        }
        return sb.toString();
    }
}
