package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inventory Count Line entity representing individual items in an inventory count.
 */
@Entity
@Table(name = "inventory_count_lines", indexes = {
    @Index(name = "idx_inv_count_line_tenant", columnList = "tenant_id"),
    @Index(name = "idx_inv_count_line_count", columnList = "inventory_count_id"),
    @Index(name = "idx_inv_count_line_product", columnList = "product_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InventoryCountLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "system_quantity", precision = 18, scale = 4)
    private BigDecimal systemQuantity;

    @Column(name = "counted_quantity", precision = 18, scale = 4)
    private BigDecimal countedQuantity;

    @Column(name = "variance_quantity", precision = 18, scale = 4)
    private BigDecimal varianceQuantity;

    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "variance_value", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal varianceValue = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UnitOfMeasure uom;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean counted = false;

    @Column(name = "counted_by")
    private Long countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(name = "recount_required")
    @Builder.Default
    private boolean recountRequired = false;

    @Column(name = "recount_quantity", precision = 18, scale = 4)
    private BigDecimal recountQuantity;

    @Column(name = "recounted_by")
    private Long recountedBy;

    @Column(name = "recounted_at")
    private Instant recountedAt;

    @Column(length = 500)
    private String notes;

    @Column(name = "adjustment_reason", length = 200)
    private String adjustmentReason;

    @Column(name = "stock_movement_id")
    private Long stockMovementId;

    @Column(name = "adjustment_approved")
    @Builder.Default
    private boolean adjustmentApproved = false;

    public void recordCount(BigDecimal quantity, Long userId) {
        this.countedQuantity = quantity;
        this.counted = true;
        this.countedBy = userId;
        this.countedAt = Instant.now();
        calculateVariance();
    }

    public void recordRecount(BigDecimal quantity, Long userId) {
        this.recountQuantity = quantity;
        this.countedQuantity = quantity;
        this.recountRequired = false;
        this.recountedBy = userId;
        this.recountedAt = Instant.now();
        calculateVariance();
    }

    public void calculateVariance() {
        if (systemQuantity != null && countedQuantity != null) {
            this.varianceQuantity = countedQuantity.subtract(systemQuantity);
            if (unitCost != null) {
                this.varianceValue = varianceQuantity.multiply(unitCost);
            }
        }
    }

    public boolean hasVariance() {
        return varianceQuantity != null && varianceQuantity.compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean isPositiveVariance() {
        return varianceQuantity != null && varianceQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegativeVariance() {
        return varianceQuantity != null && varianceQuantity.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getVariancePercentage() {
        if (systemQuantity == null || systemQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return varianceQuantity
                .divide(systemQuantity, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
