package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Journal Line entity representing a line item in a journal entry.
 * Each line affects one account with either a debit or credit amount.
 */
@Entity
@Table(name = "journal_lines", indexes = {
    @Index(name = "idx_journal_line_tenant", columnList = "tenant_id"),
    @Index(name = "idx_journal_line_entry", columnList = "journal_entry_id"),
    @Index(name = "idx_journal_line_account", columnList = "account_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class JournalLine extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(length = 500)
    private String description;

    @Column(name = "debit_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "base_debit_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal baseDebitAmount = BigDecimal.ZERO;

    @Column(name = "base_credit_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal baseCreditAmount = BigDecimal.ZERO;

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    @Column(name = "project_code", length = 50)
    private String projectCode;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "tax_amount", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /**
     * Checks if this is a debit entry.
     */
    public boolean isDebit() {
        return debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this is a credit entry.
     */
    public boolean isCredit() {
        return creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Gets the net amount (debit - credit).
     */
    public BigDecimal getNetAmount() {
        return debitAmount.subtract(creditAmount);
    }

    /**
     * Gets the absolute amount regardless of debit/credit.
     */
    public BigDecimal getAmount() {
        if (isDebit()) {
            return debitAmount;
        }
        return creditAmount;
    }

    /**
     * Sets the amount as a debit.
     */
    public void setAsDebit(BigDecimal amount) {
        this.debitAmount = amount;
        this.creditAmount = BigDecimal.ZERO;
    }

    /**
     * Sets the amount as a credit.
     */
    public void setAsCredit(BigDecimal amount) {
        this.debitAmount = BigDecimal.ZERO;
        this.creditAmount = amount;
    }
}
