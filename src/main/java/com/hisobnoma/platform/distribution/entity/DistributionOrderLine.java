package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A single product line on a distribution order. Prices are snapshotted
 * server-side at create/edit time (never trusted from the client).
 */
@Entity
@Table(name = "distribution_order_lines", indexes = {
    @Index(name = "idx_dist_order_lines_tenant", columnList = "tenant_id"),
    @Index(name = "idx_dist_order_lines_order", columnList = "order_id"),
    @Index(name = "idx_dist_order_lines_product", columnList = "product_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionOrderLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private DistributionOrder order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_sku", length = 50)
    private String productSku;

    @Column(name = "unit_name", length = 50)
    private String unitName;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    /** Quantity actually delivered; supports partial fulfilment. */
    @Column(name = "fulfilled_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal fulfilledQuantity = BigDecimal.ZERO;

    /** qty * unitPrice, less the line discount percent. */
    public void recalculate() {
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal pct = discountPercent != null ? discountPercent : BigDecimal.ZERO;
        BigDecimal discount = gross.multiply(pct).movePointLeft(2);
        this.lineTotal = gross.subtract(discount).max(BigDecimal.ZERO);
    }
}
