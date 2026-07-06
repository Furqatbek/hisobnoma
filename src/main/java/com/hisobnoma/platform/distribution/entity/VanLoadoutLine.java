package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * One product on a van loadout. {@code quantityLoaded} is set at load time;
 * {@code quantityReturned} / {@code quantityDamaged} are entered at reconciliation,
 * and {@code quantitySold} is derived (loaded − returned − damaged).
 */
@Entity
@Table(name = "distribution_van_loadout_lines", indexes = {
    @Index(name = "idx_van_lines_tenant", columnList = "tenant_id"),
    @Index(name = "idx_van_lines_loadout", columnList = "loadout_id"),
    @Index(name = "idx_van_lines_product", columnList = "product_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VanLoadoutLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loadout_id", nullable = false)
    private VanLoadout loadout;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_sku", length = 50)
    private String productSku;

    @Column(name = "unit_name", length = 50)
    private String unitName;

    @Column(name = "quantity_loaded", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityLoaded;

    @Column(name = "quantity_returned", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal quantityReturned = BigDecimal.ZERO;

    @Column(name = "quantity_damaged", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal quantityDamaged = BigDecimal.ZERO;

    @Column(name = "quantity_sold", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal quantitySold = BigDecimal.ZERO;

    /** Cost snapshot at load (for stock valuation). */
    @Column(name = "unit_cost", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    /** Selling-price snapshot at load (drives expected cash). */
    @Column(name = "unit_price", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;
}
