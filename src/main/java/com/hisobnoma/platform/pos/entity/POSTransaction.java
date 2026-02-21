package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.finance.entity.Customer;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * POS Transaction entity.
 * Represents a point of sale transaction (sale, return, exchange).
 */
@Entity
@Table(name = "pos_transactions", indexes = {
    @Index(name = "idx_pos_transactions_tenant", columnList = "tenant_id"),
    @Index(name = "idx_pos_transactions_number", columnList = "transaction_number"),
    @Index(name = "idx_pos_transactions_terminal", columnList = "terminal_id"),
    @Index(name = "idx_pos_transactions_shift", columnList = "shift_id"),
    @Index(name = "idx_pos_transactions_customer", columnList = "customer_id"),
    @Index(name = "idx_pos_transactions_status", columnList = "status"),
    @Index(name = "idx_pos_transactions_type", columnList = "transaction_type"),
    @Index(name = "idx_pos_transactions_completed_at", columnList = "completed_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class POSTransaction extends TenantAwareEntity {

    @Column(name = "transaction_number", nullable = false, length = 50)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private POSTerminal terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "cashier_name", length = 200)
    private String cashierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Builder.Default
    private TransactionType transactionType = TransactionType.SALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_reason", length = 200)
    private String discountReason;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "item_count")
    @Builder.Default
    private Integer itemCount = 0;

    @Column(name = "line_count")
    @Builder.Default
    private Integer lineCount = 0;

    @Column(name = "held_at")
    private Instant heldAt;

    @Column(name = "held_by")
    private Long heldBy;

    @Column(name = "held_reason", length = 200)
    private String heldReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    /**
     * For returns - reference to original transaction
     */
    @Column(name = "original_transaction_id")
    private Long originalTransactionId;

    @Column(name = "original_transaction_number", length = 50)
    private String originalTransactionNumber;

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    /**
     * Link to AR invoice if credit sale
     */
    @Column(name = "ar_invoice_id")
    private Long arInvoiceId;

    /**
     * Link to GL journal entry
     */
    @Column(name = "gl_journal_entry_id")
    private Long glJournalEntryId;

    @Column(name = "gl_posted")
    @Builder.Default
    private boolean glPosted = false;

    @Column(name = "stock_deducted")
    @Builder.Default
    private boolean stockDeducted = false;

    @Column(name = "delivery_region_id")
    private Long deliveryRegionId;

    @Column(name = "delivery_region_name", length = 100)
    private String deliveryRegionName;

    @Column(name = "delivery_village_id")
    private Long deliveryVillageId;

    @Column(name = "delivery_village_name", length = 100)
    private String deliveryVillageName;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<POSTransactionLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<POSPayment> payments = new ArrayList<>();

    public void addLine(POSTransactionLine line) {
        lines.add(line);
        line.setTransaction(this);
        recalculateTotals();
    }

    public void removeLine(POSTransactionLine line) {
        lines.remove(line);
        line.setTransaction(null);
        recalculateTotals();
    }

    public void addPayment(POSPayment payment) {
        payments.add(payment);
        payment.setTransaction(this);
        recalculatePaidAmount();
    }

    public void removePayment(POSPayment payment) {
        payments.remove(payment);
        payment.setTransaction(null);
        recalculatePaidAmount();
    }

    public void recalculateTotals() {
        this.subtotal = lines.stream()
                .map(POSTransactionLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.taxAmount = lines.stream()
                .map(l -> l.getTaxAmount() != null ? l.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.itemCount = lines.stream()
                .mapToInt(l -> l.getQuantity() != null ? l.getQuantity().intValue() : 0)
                .sum();

        this.lineCount = lines.size();

        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        if (transactionType == TransactionType.RETURN) {
            // For returns, subtotal and taxAmount are negative (from negated line totals).
            // Discount reduces the refund, so we add it back (making totalAmount less negative).
            this.totalAmount = subtotal.add(discount).add(taxAmount);
        } else {
            this.totalAmount = subtotal.subtract(discount).add(taxAmount);
        }

        recalculateChange();
    }

    public void recalculatePaidAmount() {
        this.paidAmount = payments.stream()
                .filter(p -> p.getStatus() == POSPaymentStatus.APPROVED)
                .map(POSPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        recalculateChange();
    }

    public void recalculateChange() {
        if (paidAmount == null || totalAmount == null) {
            this.changeAmount = BigDecimal.ZERO;
            return;
        }
        // For returns, totalAmount is negative — change doesn't apply (customer gets a refund).
        // Only compute change for sale/exchange where totalAmount > 0 and customer overpays.
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(totalAmount) > 0) {
            this.changeAmount = paidAmount.subtract(totalAmount);
        } else {
            this.changeAmount = BigDecimal.ZERO;
        }
    }

    public BigDecimal getBalanceDue() {
        return totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }

    public boolean isFullyPaid() {
        return getBalanceDue().compareTo(BigDecimal.ZERO) <= 0;
    }

    public void applyPercentDiscount(BigDecimal percent, String reason) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 100");
        }
        this.discountPercent = percent;
        this.discountReason = reason;
        // Use absolute subtotal so this works for both sales (positive) and returns (negative)
        this.discountAmount = subtotal.abs().multiply(percent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        recalculateTotals();
    }

    public void applyFixedDiscount(BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount amount must be non-negative");
        }
        // Compare against absolute subtotal — return subtotals are negative
        BigDecimal absSubtotal = subtotal != null ? subtotal.abs() : BigDecimal.ZERO;
        if (amount.compareTo(absSubtotal) > 0) {
            throw new IllegalArgumentException("Discount amount cannot exceed subtotal of " + absSubtotal);
        }
        this.discountAmount = amount;
        this.discountReason = reason;
        this.discountPercent = null;
        recalculateTotals();
    }
}
