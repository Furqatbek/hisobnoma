package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bank Account entity representing bank accounts for cash management.
 * Links to GL accounts for automatic posting.
 */
@Entity
@Table(name = "bank_accounts", indexes = {
    @Index(name = "idx_bank_account_tenant", columnList = "tenant_id"),
    @Index(name = "idx_bank_account_code", columnList = "account_code"),
    @Index(name = "idx_bank_account_type", columnList = "account_type"),
    @Index(name = "idx_bank_account_gl", columnList = "gl_account_id"),
    @Index(name = "idx_bank_account_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BankAccount extends TenantAwareEntity {

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private BankAccountType accountType;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "bank_branch", length = 200)
    private String bankBranch;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "iban", length = 50)
    private String iban;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "current_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "opening_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;

    @Column(name = "last_reconciled_date")
    private LocalDate lastReconciledDate;

    @Column(name = "last_reconciled_balance", precision = 18, scale = 4)
    private BigDecimal lastReconciledBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_account_id")
    private Account glAccount;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultAccount = false;

    @Column(name = "allow_payments", nullable = false)
    @Builder.Default
    private boolean allowPayments = true;

    @Column(name = "allow_receipts", nullable = false)
    @Builder.Default
    private boolean allowReceipts = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL)
    @OrderBy("transactionDate DESC, id DESC")
    @Builder.Default
    private List<BankTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL)
    @OrderBy("statementDate DESC")
    @Builder.Default
    private List<BankReconciliation> reconciliations = new ArrayList<>();

    /**
     * Updates the current balance by adding the given amount.
     * Positive amount increases balance, negative decreases.
     */
    public void updateBalance(BigDecimal amount) {
        this.currentBalance = this.currentBalance.add(amount);
        this.availableBalance = this.currentBalance;
    }
}
