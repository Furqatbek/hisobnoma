package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Receiving Order entity for tracking goods received from vendors.
 */
@Entity
@Table(name = "receiving_orders", indexes = {
    @Index(name = "idx_receiving_tenant", columnList = "tenant_id"),
    @Index(name = "idx_receiving_number", columnList = "receiving_number"),
    @Index(name = "idx_receiving_po", columnList = "purchase_order_id"),
    @Index(name = "idx_receiving_vendor", columnList = "vendor_id"),
    @Index(name = "idx_receiving_status", columnList = "status"),
    @Index(name = "idx_receiving_date", columnList = "receiving_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ReceivingOrder extends TenantAwareEntity {

    @Column(name = "receiving_number", nullable = false, unique = true, length = 50)
    private String receivingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReceivingStatus status = ReceivingStatus.DRAFT;

    @Column(name = "receiving_date", nullable = false)
    private LocalDate receivingDate;

    @Column(name = "vendor_delivery_note", length = 100)
    private String vendorDeliveryNote;

    @Column(name = "vendor_invoice_number", length = 100)
    private String vendorInvoiceNumber;

    @Column(name = "vendor_invoice_date")
    private LocalDate vendorInvoiceDate;

    @Column(name = "subtotal", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

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

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "stock_updated")
    @Builder.Default
    private boolean stockUpdated = false;

    @Column(name = "ap_invoice_created")
    @Builder.Default
    private boolean apInvoiceCreated = false;

    @Column(name = "ap_invoice_id")
    private Long apInvoiceId;

    @OneToMany(mappedBy = "receivingOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReceivingLine> lines = new ArrayList<>();

    public void addLine(ReceivingLine line) {
        lines.add(line);
        line.setReceivingOrder(this);
        recalculateTotals();
    }

    public void removeLine(ReceivingLine line) {
        lines.remove(line);
        line.setReceivingOrder(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = lines.stream()
                .map(ReceivingLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.taxAmount = lines.stream()
                .map(ReceivingLine::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .add(taxAmount)
                .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);
    }

    public BigDecimal getTotalReceivedQuantity() {
        return lines.stream()
                .map(ReceivingLine::getReceivedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasVariances() {
        return lines.stream().anyMatch(line -> line.getVarianceQuantity().compareTo(BigDecimal.ZERO) != 0);
    }
}
