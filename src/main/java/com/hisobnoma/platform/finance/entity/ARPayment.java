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
 * Accounts Receivable Payment entity.
 * Represents payments received from customers.
 */
@Entity
@Table(name = "ar_payments", indexes = {
    @Index(name = "idx_ar_payments_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ar_payments_number", columnList = "payment_number"),
    @Index(name = "idx_ar_payments_customer", columnList = "customer_id"),
    @Index(name = "idx_ar_payments_status", columnList = "status"),
    @Index(name = "idx_ar_payments_date", columnList = "payment_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ARPayment extends TenantAwareEntity {

    @Column(name = "payment_number", nullable = false, length = 50)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ARPaymentStatus status = ARPaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private ARPaymentMethod paymentMethod;

    @Column(name = "payment_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal paymentAmount;

    /**
     * Amount allocated to invoices.
     */
    @Column(name = "allocated_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    /**
     * Amount not yet allocated.
     */
    @Column(name = "unallocated_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /**
     * Bank account for deposit (if applicable).
     */
    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "bank_account_name", length = 200)
    private String bankAccountName;

    /**
     * Cash account for cash receipts.
     */
    @Column(name = "cash_account_id")
    private Long cashAccountId;

    /**
     * AR control account.
     */
    @Column(name = "ar_account_id")
    private Long arAccountId;

    /**
     * Reference number (check number, transaction ID, etc.).
     */
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    /**
     * Check number if payment by check.
     */
    @Column(name = "check_number", length = 50)
    private String checkNumber;

    @Column(name = "check_date")
    private LocalDate checkDate;

    /**
     * Card last 4 digits for card payments.
     */
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_type", length = 20)
    private String cardType;

    /**
     * Transaction ID from payment gateway.
     */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(length = 200)
    private String memo;

    @Column(length = 1000)
    private String notes;

    /**
     * Whether this payment has been posted to GL.
     */
    @Column(name = "gl_posted")
    @Builder.Default
    private boolean glPosted = false;

    @Column(name = "gl_journal_entry_id")
    private Long glJournalEntryId;

    @Column(name = "gl_posted_at")
    private Instant glPostedAt;

    /**
     * Whether this payment has been deposited/reconciled.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deposited = false;

    @Column(name = "deposited_at")
    private Instant depositedAt;

    @Column(name = "deposited_by")
    private Long depositedBy;

    @Column(name = "deposit_reference", length = 100)
    private String depositReference;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "refunded_by")
    private Long refundedBy;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "arPayment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ARPaymentAllocation> allocations = new ArrayList<>();

    public void addAllocation(ARPaymentAllocation allocation) {
        allocations.add(allocation);
        allocation.setArPayment(this);
        recalculateAllocations();
    }

    public void removeAllocation(ARPaymentAllocation allocation) {
        allocations.remove(allocation);
        allocation.setArPayment(null);
        recalculateAllocations();
    }

    public void recalculateAllocations() {
        this.allocatedAmount = allocations.stream()
                .map(ARPaymentAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.unallocatedAmount = paymentAmount.subtract(allocatedAmount);
    }

    public boolean isFullyAllocated() {
        return unallocatedAmount.compareTo(BigDecimal.ZERO) <= 0;
    }

    public BigDecimal getAvailableAmount() {
        return unallocatedAmount.compareTo(BigDecimal.ZERO) > 0 ? unallocatedAmount : BigDecimal.ZERO;
    }
}
