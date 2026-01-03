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
 * AP Payment entity.
 * Represents payments made to vendors for AP invoices.
 */
@Entity
@Table(name = "ap_payments", indexes = {
    @Index(name = "idx_ap_payments_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ap_payments_number", columnList = "payment_number"),
    @Index(name = "idx_ap_payments_vendor", columnList = "vendor_id"),
    @Index(name = "idx_ap_payments_status", columnList = "status"),
    @Index(name = "idx_ap_payments_date", columnList = "payment_date"),
    @Index(name = "idx_ap_payments_bank", columnList = "bank_account_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class APPayment extends TenantAwareEntity {

    @Column(name = "payment_number", nullable = false, unique = true, length = 50)
    private String paymentNumber;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private APPaymentStatus status = APPaymentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private APPaymentMethod paymentMethod;

    @Column(name = "payment_amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal paymentAmount;

    @Column(name = "allocated_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

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
     * Bank account used for payment.
     */
    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "bank_account_name", length = 200)
    private String bankAccountName;

    /**
     * GL cash/bank account for payment.
     */
    @Column(name = "cash_account_id")
    private Long cashAccountId;

    /**
     * AP control account.
     */
    @Column(name = "ap_account_id")
    private Long apAccountId;

    /**
     * Check/reference number.
     */
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "check_number", length = 50)
    private String checkNumber;

    @Column(name = "check_date")
    private LocalDate checkDate;

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
     * Whether this payment has been reconciled with bank statement.
     */
    @Column(name = "reconciled")
    @Builder.Default
    private boolean reconciled = false;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @Column(name = "reconciled_by")
    private Long reconciledBy;

    // Approval workflow
    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @OneToMany(mappedBy = "apPayment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<APPaymentAllocation> allocations = new ArrayList<>();

    public void addAllocation(APPaymentAllocation allocation) {
        allocations.add(allocation);
        allocation.setApPayment(this);
        recalculateAllocations();
    }

    public void removeAllocation(APPaymentAllocation allocation) {
        allocations.remove(allocation);
        allocation.setApPayment(null);
        recalculateAllocations();
    }

    public void recalculateAllocations() {
        this.allocatedAmount = allocations.stream()
                .map(APPaymentAllocation::getAllocatedAmount)
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
