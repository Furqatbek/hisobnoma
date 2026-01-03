package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Order entity for ordering products from vendors.
 */
@Entity
@Table(name = "purchase_orders", indexes = {
    @Index(name = "idx_po_tenant", columnList = "tenant_id"),
    @Index(name = "idx_po_number", columnList = "po_number"),
    @Index(name = "idx_po_vendor", columnList = "vendor_id"),
    @Index(name = "idx_po_status", columnList = "status"),
    @Index(name = "idx_po_order_date", columnList = "order_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PurchaseOrder extends TenantAwareEntity {

    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private POStatus status = POStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "subtotal", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    @Column(name = "vendor_reference", length = 100)
    private String vendorReference;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private java.time.Instant approvedAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private java.time.Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
        recalculateTotals();
    }

    public void removeLine(PurchaseOrderLine line) {
        lines.remove(line);
        line.setPurchaseOrder(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = lines.stream()
                .map(PurchaseOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discount = subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        } else if (discountAmount != null) {
            discount = discountAmount;
        }
        this.discountAmount = discount;

        BigDecimal afterDiscount = subtotal.subtract(discount);
        this.totalAmount = afterDiscount
                .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);
    }

    public BigDecimal getTotalReceivedQuantity() {
        return lines.stream()
                .map(PurchaseOrderLine::getReceivedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isFullyReceived() {
        return lines.stream().allMatch(line ->
            line.getReceivedQuantity().compareTo(line.getQuantity()) >= 0);
    }

    public boolean isPartiallyReceived() {
        BigDecimal totalReceived = getTotalReceivedQuantity();
        return totalReceived.compareTo(BigDecimal.ZERO) > 0 && !isFullyReceived();
    }
}
