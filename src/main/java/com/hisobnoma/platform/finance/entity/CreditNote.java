package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Credit Note entity for customer refunds and adjustments.
 */
@Entity
@Table(name = "credit_notes", indexes = {
    @Index(name = "idx_credit_notes_tenant", columnList = "tenant_id"),
    @Index(name = "idx_credit_notes_number", columnList = "credit_note_number"),
    @Index(name = "idx_credit_notes_customer", columnList = "customer_id"),
    @Index(name = "idx_credit_notes_invoice", columnList = "original_invoice_id"),
    @Index(name = "idx_credit_notes_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CreditNote extends TenantAwareEntity {

    @Column(name = "credit_note_number", nullable = false, length = 50)
    private String creditNoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    /**
     * Original invoice that this credit note is against (optional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_invoice_id")
    private ARInvoice originalInvoice;

    @Column(name = "original_invoice_number", length = 50)
    private String originalInvoiceNumber;

    @Column(name = "credit_note_date", nullable = false)
    private LocalDate creditNoteDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CreditNoteStatus status = CreditNoteStatus.DRAFT;

    /**
     * Reason for the credit note.
     */
    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(length = 500)
    private String reason;

    /**
     * The credit amount (total credit to customer).
     */
    @Column(name = "credit_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Amount already applied to invoices.
     */
    @Column(name = "applied_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    /**
     * Remaining balance available to apply.
     */
    @Column(name = "balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Amount refunded in cash/card.
     */
    @Column(name = "refunded_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /**
     * AR control account.
     */
    @Column(name = "ar_account_id")
    private Long arAccountId;

    /**
     * Revenue account for credit (contra-revenue).
     */
    @Column(name = "revenue_account_id")
    private Long revenueAccountId;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String notes;

    /**
     * Whether this credit note has been posted to GL.
     */
    @Column(name = "gl_posted")
    @Builder.Default
    private boolean glPosted = false;

    @Column(name = "gl_journal_entry_id")
    private Long glJournalEntryId;

    @Column(name = "gl_posted_at")
    private Instant glPostedAt;

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

    /**
     * Apply amount to an invoice.
     */
    public void applyToInvoice(BigDecimal amount) {
        this.appliedAmount = (this.appliedAmount != null ? this.appliedAmount : BigDecimal.ZERO).add(amount);
        this.balance = this.totalAmount.subtract(this.appliedAmount).subtract(
                this.refundedAmount != null ? this.refundedAmount : BigDecimal.ZERO);

        if (this.balance.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = CreditNoteStatus.APPLIED;
        } else if (this.appliedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = CreditNoteStatus.PARTIAL;
        }
    }

    /**
     * Record a refund from this credit note.
     */
    public void recordRefund(BigDecimal amount) {
        this.refundedAmount = (this.refundedAmount != null ? this.refundedAmount : BigDecimal.ZERO).add(amount);
        this.balance = this.totalAmount.subtract(
                this.appliedAmount != null ? this.appliedAmount : BigDecimal.ZERO)
                .subtract(this.refundedAmount);

        if (this.balance.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = CreditNoteStatus.APPLIED;
        } else if (this.appliedAmount.compareTo(BigDecimal.ZERO) > 0 || this.refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = CreditNoteStatus.PARTIAL;
        }
    }
}
