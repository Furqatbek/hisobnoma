package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * AR Payment Allocation entity.
 * Links payments to specific invoices and tracks how much is applied.
 */
@Entity
@Table(name = "ar_payment_allocations", indexes = {
    @Index(name = "idx_ar_alloc_payment", columnList = "ar_payment_id"),
    @Index(name = "idx_ar_alloc_invoice", columnList = "ar_invoice_id"),
    @Index(name = "idx_ar_alloc_credit_note", columnList = "credit_note_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ARPaymentAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_payment_id", nullable = false)
    private ARPayment arPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_invoice_id")
    private ARInvoice arInvoice;

    /**
     * Credit note being applied (if applicable).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id")
    private CreditNote creditNote;

    /**
     * Invoice number for denormalized reference.
     */
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    /**
     * Invoice total amount for reference.
     */
    @Column(name = "invoice_amount", precision = 18, scale = 4)
    private BigDecimal invoiceAmount;

    /**
     * Invoice balance before this allocation.
     */
    @Column(name = "invoice_balance_before", precision = 18, scale = 4)
    private BigDecimal invoiceBalanceBefore;

    /**
     * Amount allocated from the payment to this invoice.
     */
    @Column(name = "allocated_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal allocatedAmount;

    /**
     * Discount taken for early payment.
     */
    @Column(name = "discount_taken", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountTaken = BigDecimal.ZERO;

    /**
     * Amount written off as bad debt.
     */
    @Column(name = "write_off_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal writeOffAmount = BigDecimal.ZERO;

    /**
     * Invoice balance after this allocation.
     */
    @Column(name = "invoice_balance_after", precision = 18, scale = 4)
    private BigDecimal invoiceBalanceAfter;

    @Column(length = 500)
    private String notes;

    /**
     * Calculate the invoice balance after applying this allocation.
     */
    public void calculateBalanceAfter() {
        BigDecimal before = invoiceBalanceBefore != null ? invoiceBalanceBefore : BigDecimal.ZERO;
        BigDecimal allocated = allocatedAmount != null ? allocatedAmount : BigDecimal.ZERO;
        BigDecimal discount = discountTaken != null ? discountTaken : BigDecimal.ZERO;
        BigDecimal writeOff = writeOffAmount != null ? writeOffAmount : BigDecimal.ZERO;

        this.invoiceBalanceAfter = before.subtract(allocated).subtract(discount).subtract(writeOff);
    }

    /**
     * Get total amount applied (payment + discount + write-off).
     */
    public BigDecimal getTotalApplied() {
        BigDecimal allocated = allocatedAmount != null ? allocatedAmount : BigDecimal.ZERO;
        BigDecimal discount = discountTaken != null ? discountTaken : BigDecimal.ZERO;
        BigDecimal writeOff = writeOffAmount != null ? writeOffAmount : BigDecimal.ZERO;

        return allocated.add(discount).add(writeOff);
    }
}
