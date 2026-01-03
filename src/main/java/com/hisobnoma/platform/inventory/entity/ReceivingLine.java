package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Receiving Line entity representing individual items in a receiving order.
 */
@Entity
@Table(name = "receiving_lines", indexes = {
    @Index(name = "idx_receiving_line_tenant", columnList = "tenant_id"),
    @Index(name = "idx_receiving_line_receiving", columnList = "receiving_order_id"),
    @Index(name = "idx_receiving_line_product", columnList = "product_id"),
    @Index(name = "idx_receiving_line_po_line", columnList = "po_line_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ReceivingLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_order_id", nullable = false)
    private ReceivingOrder receivingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id")
    private PurchaseOrderLine poLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(length = 500)
    private String description;

    @Column(name = "expected_quantity", precision = 18, scale = 4)
    private BigDecimal expectedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "accepted_quantity", precision = 18, scale = 4)
    private BigDecimal acceptedQuantity;

    @Column(name = "rejected_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UnitOfMeasure uom;

    @Column(name = "unit_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "serial_numbers", length = 2000)
    private String serialNumbers;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_location_id")
    private Location targetLocation;

    @Column(length = 500)
    private String notes;

    @Column(name = "stock_movement_id")
    private Long stockMovementId;

    @PrePersist
    @PreUpdate
    public void calculate() {
        if (acceptedQuantity == null) {
            acceptedQuantity = receivedQuantity.subtract(rejectedQuantity != null ? rejectedQuantity : BigDecimal.ZERO);
        }

        BigDecimal netAmount = acceptedQuantity.multiply(unitCost);

        if (taxPercent != null && taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = netAmount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        }

        this.lineTotal = netAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }

    public BigDecimal getVarianceQuantity() {
        if (expectedQuantity == null) {
            return BigDecimal.ZERO;
        }
        return receivedQuantity.subtract(expectedQuantity);
    }

    public boolean hasVariance() {
        return getVarianceQuantity().compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean isOverReceived() {
        return getVarianceQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isUnderReceived() {
        return getVarianceQuantity().compareTo(BigDecimal.ZERO) < 0;
    }
}
