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
 * Accounts Receivable Invoice entity.
 * Represents invoices issued to customers for goods/services.
 */
@Entity
@Table(name = "ar_invoices", indexes = {
    @Index(name = "idx_ar_invoices_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ar_invoices_number", columnList = "invoice_number"),
    @Index(name = "idx_ar_invoices_customer", columnList = "customer_id"),
    @Index(name = "idx_ar_invoices_status", columnList = "status"),
    @Index(name = "idx_ar_invoices_due_date", columnList = "due_date"),
    @Index(name = "idx_ar_invoices_pos", columnList = "pos_transaction_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ARInvoice extends TenantAwareEntity {

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ARInvoiceStatus status = ARInvoiceStatus.DRAFT;

    /**
     * Link to POS transaction that generated this invoice.
     */
    @Column(name = "pos_transaction_id")
    private Long posTransactionId;

    @Column(name = "pos_transaction_number", length = 50)
    private String posTransactionNumber;

    /**
     * Link to Sales Order if applicable.
     */
    @Column(name = "sales_order_id")
    private Long salesOrderId;

    @Column(name = "sales_order_number", length = 50)
    private String salesOrderNumber;

    @Column(name = "subtotal", precision = 18, scale = 4, nullable = false)
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

    @Column(name = "total_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "credit_applied", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal creditApplied = BigDecimal.ZERO;

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
     * AR control account (Accounts Receivable).
     */
    @Column(name = "ar_account_id")
    private Long arAccountId;

    /**
     * Revenue account for this invoice.
     */
    @Column(name = "revenue_account_id")
    private Long revenueAccountId;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String notes;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    /**
     * Billing address for the invoice.
     */
    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    /**
     * Shipping address if different from billing.
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

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

    /**
     * Date when invoice was sent to customer.
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "sent_by")
    private Long sentBy;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "written_off_by")
    private Long writtenOffBy;

    @Column(name = "written_off_at")
    private Instant writtenOffAt;

    @Column(name = "write_off_reason", length = 500)
    private String writeOffReason;

    @OneToMany(mappedBy = "arInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ARInvoiceLine> lines = new ArrayList<>();

    public void addLine(ARInvoiceLine line) {
        lines.add(line);
        line.setArInvoice(this);
        recalculateTotals();
    }

    public void removeLine(ARInvoiceLine line) {
        lines.remove(line);
        line.setArInvoice(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = lines.stream()
                .map(ARInvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.taxAmount = lines.stream()
                .map(l -> l.getTaxAmount() != null ? l.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal
                .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
                .add(taxAmount)
                .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);

        updateBalanceDue();
    }

    public void updateBalanceDue() {
        BigDecimal payments = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal credits = creditApplied != null ? creditApplied : BigDecimal.ZERO;
        this.balanceDue = totalAmount.subtract(payments).subtract(credits);

        // Update status based on balance (skip auto-PAID for zero-amount invoices like item lists without prices)
        if (this.balanceDue.compareTo(BigDecimal.ZERO) <= 0 && this.totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (this.status != ARInvoiceStatus.CANCELLED && this.status != ARInvoiceStatus.WRITTEN_OFF) {
                this.status = ARInvoiceStatus.PAID;
            }
        } else if (payments.compareTo(BigDecimal.ZERO) > 0 || credits.compareTo(BigDecimal.ZERO) > 0) {
            if (this.status != ARInvoiceStatus.CANCELLED && this.status != ARInvoiceStatus.WRITTEN_OFF) {
                this.status = ARInvoiceStatus.PARTIAL;
            }
        }
    }

    public void applyPayment(BigDecimal amount) {
        this.paidAmount = (this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO).add(amount);
        updateBalanceDue();
    }

    public void applyCredit(BigDecimal amount) {
        this.creditApplied = (this.creditApplied != null ? this.creditApplied : BigDecimal.ZERO).add(amount);
        updateBalanceDue();
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDate.now())
                && status != ARInvoiceStatus.PAID
                && status != ARInvoiceStatus.CANCELLED
                && status != ARInvoiceStatus.WRITTEN_OFF;
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
