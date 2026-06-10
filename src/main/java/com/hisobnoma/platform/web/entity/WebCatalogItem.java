package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.inventory.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Curated item of the online shop catalog.
 * References a product and controls its visibility (draft/live),
 * display order and optional name/price overrides for the shop.
 */
@Entity
@Table(name = "web_catalog_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_web_catalog_product", columnNames = {"tenant_id", "product_id"}),
        indexes = {
                @Index(name = "idx_web_catalog_tenant", columnList = "tenant_id"),
                @Index(name = "idx_web_catalog_status", columnList = "tenant_id, status"),
                @Index(name = "idx_web_catalog_sort", columnList = "tenant_id, sort_order")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebCatalogItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Optional name shown in the shop instead of the product name.
     */
    @Column(name = "display_name", length = 200)
    private String displayName;

    /**
     * Optional price override; falls back to the product's selling price.
     */
    @Column(name = "price_override", precision = 18, scale = 4)
    private BigDecimal priceOverride;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WebCatalogStatus status = WebCatalogStatus.DRAFT;

    public BigDecimal getEffectivePrice() {
        return priceOverride != null ? priceOverride : product.getSellingPrice();
    }

    public String getEffectiveName() {
        return displayName != null && !displayName.isBlank() ? displayName : product.getName();
    }
}
