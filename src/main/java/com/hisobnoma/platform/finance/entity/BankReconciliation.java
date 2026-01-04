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
 * Bank Reconciliation entity for matching bank statements with recorded transactions.
 * Tracks the reconciliation process from start to completion.
 */
@Entity
@Table(name = "bank_reconciliations", indexes = {
    @Index(name = "idx_bank_recon_tenant", columnList = "tenant_id"),
    @Index(name = "idx_bank_recon_account", columnList = "bank_account_id"),
    @Index(name = "idx_bank_recon_date", columnList = "statement_date"),
    @Index(name = "idx_bank_recon_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BankReconciliation extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "reconciliation_number", nullable = false, unique = true, length = 50)
    private String reconciliationNumber;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "statement_start_date", nullable = false)
    private LocalDate statementStartDate;

    @Column(name = "statement_end_date", nullable = false)
    private LocalDate statementEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.DRAFT;

    @Column(name = "opening_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "statement_ending_balance", precision = 18, scale = 4, nullable = false)
    private BigDecimal statementEndingBalance;

    @Column(name = "book_balance", precision = 18, scale = 4)
    private BigDecimal bookBalance;

    @Column(name = "cleared_deposits", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal clearedDeposits = BigDecimal.ZERO;

    @Column(name = "cleared_withdrawals", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal clearedWithdrawals = BigDecimal.ZERO;

    @Column(name = "outstanding_deposits", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal outstandingDeposits = BigDecimal.ZERO;

    @Column(name = "outstanding_withdrawals", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal outstandingWithdrawals = BigDecimal.ZERO;

    @Column(name = "adjusted_bank_balance", precision = 18, scale = 4)
    private BigDecimal adjustedBankBalance;

    @Column(name = "adjusted_book_balance", precision = 18, scale = 4)
    private BigDecimal adjustedBookBalance;

    @Column(name = "difference", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "transactions_cleared")
    @Builder.Default
    private Integer transactionsCleared = 0;

    @Column(name = "transactions_outstanding")
    @Builder.Default
    private Integer transactionsOutstanding = 0;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL)
    @Builder.Default
    private List<BankTransaction> reconciledTransactions = new ArrayList<>();

    /**
     * Checks if the reconciliation is balanced (difference is zero).
     */
    public boolean isBalanced() {
        return difference != null && difference.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Calculates the adjusted bank balance.
     * Adjusted Bank Balance = Statement Ending Balance - Outstanding Withdrawals + Outstanding Deposits
     */
    public void calculateAdjustedBankBalance() {
        this.adjustedBankBalance = this.statementEndingBalance
            .subtract(this.outstandingWithdrawals)
            .add(this.outstandingDeposits);
    }

    /**
     * Calculates the difference between adjusted balances.
     */
    public void calculateDifference() {
        if (this.adjustedBankBalance != null && this.adjustedBookBalance != null) {
            this.difference = this.adjustedBookBalance.subtract(this.adjustedBankBalance);
        }
    }
}
