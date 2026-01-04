package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Shift entity.
 * Represents a cashier's working shift on a terminal.
 */
@Entity
@Table(name = "pos_shifts", indexes = {
    @Index(name = "idx_pos_shifts_tenant", columnList = "tenant_id"),
    @Index(name = "idx_pos_shifts_terminal", columnList = "terminal_id"),
    @Index(name = "idx_pos_shifts_cashier", columnList = "cashier_id"),
    @Index(name = "idx_pos_shifts_status", columnList = "status"),
    @Index(name = "idx_pos_shifts_opened_at", columnList = "opened_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Shift extends TenantAwareEntity {

    @Column(name = "shift_number", nullable = false, length = 50)
    private String shiftNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private POSTerminal terminal;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "cashier_name", length = 100)
    private String cashierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "opening_cash", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closing_cash", precision = 18, scale = 4)
    private BigDecimal closingCash;

    @Column(name = "expected_cash", precision = 18, scale = 4)
    private BigDecimal expectedCash;

    @Column(name = "cash_difference", precision = 18, scale = 4)
    private BigDecimal cashDifference;

    @Column(name = "total_sales", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_returns", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalReturns = BigDecimal.ZERO;

    @Column(name = "total_discounts", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalDiscounts = BigDecimal.ZERO;

    @Column(name = "total_taxes", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalTaxes = BigDecimal.ZERO;

    @Column(name = "cash_payments", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cashPayments = BigDecimal.ZERO;

    @Column(name = "card_payments", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cardPayments = BigDecimal.ZERO;

    @Column(name = "other_payments", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal otherPayments = BigDecimal.ZERO;

    @Column(name = "transaction_count")
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "voided_count")
    @Builder.Default
    private Integer voidedCount = 0;

    @Column(name = "return_count")
    @Builder.Default
    private Integer returnCount = 0;

    @Column(name = "cash_in", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cashIn = BigDecimal.ZERO;

    @Column(name = "cash_out", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal cashOut = BigDecimal.ZERO;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "closing_notes", length = 1000)
    private String closingNotes;

    public void calculateExpectedCash() {
        this.expectedCash = openingCash
                .add(cashPayments != null ? cashPayments : BigDecimal.ZERO)
                .add(cashIn != null ? cashIn : BigDecimal.ZERO)
                .subtract(cashOut != null ? cashOut : BigDecimal.ZERO);
    }

    public void calculateCashDifference() {
        if (closingCash != null && expectedCash != null) {
            this.cashDifference = closingCash.subtract(expectedCash);
        }
    }

    public void addTransaction(POSTransaction transaction) {
        this.transactionCount++;
        if (transaction.getTransactionType() == TransactionType.SALE) {
            this.totalSales = this.totalSales.add(transaction.getTotalAmount());
        } else if (transaction.getTransactionType() == TransactionType.RETURN) {
            this.totalReturns = this.totalReturns.add(transaction.getTotalAmount());
            this.returnCount++;
        }
        if (transaction.getDiscountAmount() != null) {
            this.totalDiscounts = this.totalDiscounts.add(transaction.getDiscountAmount());
        }
        if (transaction.getTaxAmount() != null) {
            this.totalTaxes = this.totalTaxes.add(transaction.getTaxAmount());
        }
    }
}
