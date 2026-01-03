package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * AP Payment Allocation entity.
 * Represents the allocation of a payment to one or more AP invoices.
 */
@Entity
@Table(name = "ap_payment_allocations", indexes = {
    @Index(name = "idx_ap_alloc_payment", columnList = "ap_payment_id"),
    @Index(name = "idx_ap_alloc_invoice", columnList = "ap_invoice_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class APPaymentAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ap_payment_id", nullable = false)
    private APPayment apPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ap_invoice_id", nullable = false)
    private APInvoice apInvoice;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_amount", precision = 18, scale = 4)
    private BigDecimal invoiceAmount;

    @Column(name = "invoice_balance_before", precision = 18, scale = 4)
    private BigDecimal invoiceBalanceBefore;

    @Column(name = "allocated_amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "discount_taken", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountTaken = BigDecimal.ZERO;

    @Column(name = "write_off_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal writeOffAmount = BigDecimal.ZERO;

    @Column(name = "invoice_balance_after", precision = 18, scale = 4)
    private BigDecimal invoiceBalanceAfter;

    @Column(length = 500)
    private String notes;

    /**
     * Calculate the balance after this allocation.
     */
    public void calculateBalanceAfter() {
        BigDecimal totalApplied = allocatedAmount
                .add(discountTaken != null ? discountTaken : BigDecimal.ZERO)
                .add(writeOffAmount != null ? writeOffAmount : BigDecimal.ZERO);

        this.invoiceBalanceAfter = (invoiceBalanceBefore != null ? invoiceBalanceBefore : BigDecimal.ZERO)
                .subtract(totalApplied);

        if (this.invoiceBalanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            this.invoiceBalanceAfter = BigDecimal.ZERO;
        }
    }

    /**
     * Get total amount applied to invoice (payment + discount + write-off).
     */
    public BigDecimal getTotalApplied() {
        return allocatedAmount
                .add(discountTaken != null ? discountTaken : BigDecimal.ZERO)
                .add(writeOffAmount != null ? writeOffAmount : BigDecimal.ZERO);
    }
}
