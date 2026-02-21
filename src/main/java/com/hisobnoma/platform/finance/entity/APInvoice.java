package com.hisobnoma.platform.finance.entity;

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
 * Accounts Payable Invoice entity.
 * Represents invoices received from vendors for goods/services.
 */
@Entity
@Table(name = "ap_invoices", indexes = {
    @Index(name = "idx_ap_invoices_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ap_invoices_number", columnList = "invoice_number"),
    @Index(name = "idx_ap_invoices_vendor", columnList = "vendor_id"),
    @Index(name = "idx_ap_invoices_status", columnList = "status"),
    @Index(name = "idx_ap_invoices_due_date", columnList = "due_date"),
    @Index(name = "idx_ap_invoices_po", columnList = "purchase_order_id"),
    @Index(name = "idx_ap_invoices_receiving", columnList = "receiving_order_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class APInvoice extends TenantAwareEntity {

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "vendor_invoice_number", length = 100)
    private String vendorInvoiceNumber;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private APInvoiceStatus status = APInvoiceStatus.DRAFT;

    /**
     * Link to Purchase Order (for 3-way matching).
     */
    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "purchase_order_number", length = 50)
    private String purchaseOrderNumber;

    /**
     * Link to Receiving Order (for 3-way matching).
     */
    @Column(name = "receiving_order_id")
    private Long receivingOrderId;

    @Column(name = "receiving_order_number", length = 50)
    private String receivingOrderNumber;

    @Column(name = "subtotal", precision = 18, scale = 4, nullable = false)
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

    @Column(name = "total_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    /**
     * Default expense account for this invoice (from vendor or overridden).
     */
    @Column(name = "expense_account_id")
    private Long expenseAccountId;

    /**
     * AP control account (Accounts Payable).
     */
    @Column(name = "ap_account_id")
    private Long apAccountId;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    /**
     * 3-way matching status.
     */
    @Column(name = "matching_status", length = 20)
    private String matchingStatus;

    @Column(name = "matching_notes", length = 500)
    private String matchingNotes;

    /**
     * Whether this invoice has been posted to GL.
     */
    @Column(name = "gl_posted")
    @Builder.Default
    private boolean glPosted = false;

    @Column(name = "gl_journal_entry_id")
    private Long glJournalEntryId;

    @Column(name = "gl_posted_at")
    private Instant glPostedAt;

    // Approval workflow
    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "apInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<APInvoiceLine> lines = new ArrayList<>();

    public void addLine(APInvoiceLine line) {
        lines.add(line);
        line.setApInvoice(this);
        recalculateTotals();
    }

    public void removeLine(APInvoiceLine line) {
        lines.remove(line);
        line.setApInvoice(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = lines.stream()
                .map(APInvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.taxAmount = lines.stream()
                .map(l -> l.getTaxAmount() != null ? l.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .add(taxAmount)
                .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);

        this.balanceDue = totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }

    public void updatePaidAmount(BigDecimal newPayment) {
        this.paidAmount = (this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO).add(newPayment);
        this.balanceDue = totalAmount.subtract(this.paidAmount);

        // Update status based on payment
        if (this.balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = APInvoiceStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = APInvoiceStatus.PARTIAL;
        }
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                && status != APInvoiceStatus.PAID && status != APInvoiceStatus.CANCELLED;
    }

    public int getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    public int getDaysUntilDue() {
        if (dueDate == null || isOverdue()) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }
}
