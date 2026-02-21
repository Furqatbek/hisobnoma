package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Maps products to their vendors/suppliers with vendor-specific pricing and lead times.
 * A product can have multiple vendors, and one can be marked as preferred.
 */
@Entity
@Table(name = "product_vendors", indexes = {
    @Index(name = "idx_pv_tenant", columnList = "tenant_id"),
    @Index(name = "idx_pv_product", columnList = "product_id"),
    @Index(name = "idx_pv_vendor", columnList = "vendor_id"),
    @Index(name = "idx_pv_preferred", columnList = "tenant_id, product_id, is_preferred")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_pv_tenant_product_vendor", columnNames = {"tenant_id", "product_id", "vendor_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductVendor extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "vendor_sku", length = 100)
    private String vendorSku;

    @Column(name = "vendor_product_name", length = 200)
    private String vendorProductName;

    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "min_order_quantity", precision = 18, scale = 4)
    private BigDecimal minOrderQuantity;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "is_preferred", nullable = false)
    @Builder.Default
    private boolean preferred = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 1000)
    private String notes;
}
