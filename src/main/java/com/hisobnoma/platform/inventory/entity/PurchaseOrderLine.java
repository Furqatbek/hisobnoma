package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Purchase Order Line entity representing individual items in a purchase order.
 */
@Entity
@Table(name = "purchase_order_lines", indexes = {
    @Index(name = "idx_po_line_tenant", columnList = "tenant_id"),
    @Index(name = "idx_po_line_po", columnList = "purchase_order_id"),
    @Index(name = "idx_po_line_product", columnList = "product_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PurchaseOrderLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

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

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "received_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "cancelled_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cancelledQuantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UnitOfMeasure uom;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "expected_date")
    private java.time.LocalDate expectedDate;

    @PrePersist
    @PreUpdate
    public void calculateLineTotal() {
        BigDecimal gross = quantity.multiply(unitPrice);

        BigDecimal discount = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discount = gross.multiply(discountPercent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        } else if (discountAmount != null) {
            discount = discountAmount;
        }
        this.discountAmount = discount;

        BigDecimal netAmount = gross.subtract(discount);

        if (taxPercent != null && taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = netAmount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        }

        this.lineTotal = netAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }

    public BigDecimal getPendingQuantity() {
        return quantity.subtract(receivedQuantity).subtract(cancelledQuantity);
    }

    public boolean isFullyReceived() {
        return receivedQuantity.compareTo(quantity) >= 0;
    }

    public boolean isPartiallyClosed() {
        return cancelledQuantity.compareTo(BigDecimal.ZERO) > 0;
    }
}
