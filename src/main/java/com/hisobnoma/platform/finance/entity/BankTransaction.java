package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bank Transaction entity representing individual bank transactions.
 * Supports deposits, withdrawals, transfers, and other transaction types.
 */
@Entity
@Table(name = "bank_transactions", indexes = {
    @Index(name = "idx_bank_tx_tenant", columnList = "tenant_id"),
    @Index(name = "idx_bank_tx_account", columnList = "bank_account_id"),
    @Index(name = "idx_bank_tx_date", columnList = "transaction_date"),
    @Index(name = "idx_bank_tx_type", columnList = "transaction_type"),
    @Index(name = "idx_bank_tx_status", columnList = "status"),
    @Index(name = "idx_bank_tx_reference", columnList = "reference_number"),
    @Index(name = "idx_bank_tx_reconciliation", columnList = "reconciliation_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BankTransaction extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 50)
    private String transactionNumber;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private BankTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BankTransactionStatus status = BankTransactionStatus.PENDING;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "debit_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "running_balance", precision = 18, scale = 4)
    private BigDecimal runningBalance;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "base_amount", precision = 18, scale = 4)
    private BigDecimal baseAmount;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "check_number", length = 50)
    private String checkNumber;

    @Column(name = "payee_payer", length = 200)
    private String payeePayer;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_account_id")
    private Account counterAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_account_id")
    private BankAccount transferToAccount;

    @Column(name = "gl_posted", nullable = false)
    @Builder.Default
    private boolean glPosted = false;

    @Column(name = "gl_journal_entry_id")
    private Long glJournalEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id")
    private BankReconciliation reconciliation;

    @Column(name = "reconciled_date")
    private LocalDate reconciledDate;

    @Column(name = "bank_statement_line", length = 200)
    private String bankStatementLine;

    @Column(length = 1000)
    private String notes;

    /**
     * Gets the net amount (credit - debit).
     */
    public BigDecimal getNetAmount() {
        return creditAmount.subtract(debitAmount);
    }

    /**
     * Checks if this is a debit transaction.
     */
    public boolean isDebit() {
        return debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this is a credit transaction.
     */
    public boolean isCredit() {
        return creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this transaction is reconciled.
     */
    public boolean isReconciled() {
        return status == BankTransactionStatus.RECONCILED;
    }
}
