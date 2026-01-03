package com.hisobnoma.platform.finance.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chart of Accounts entity representing GL accounts.
 * Supports hierarchical structure with parent-child relationships.
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_tenant", columnList = "tenant_id"),
    @Index(name = "idx_accounts_code", columnList = "code"),
    @Index(name = "idx_accounts_type", columnList = "account_type"),
    @Index(name = "idx_accounts_parent", columnList = "parent_account_id"),
    @Index(name = "idx_accounts_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Account extends TenantAwareEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 10)
    private NormalBalance normalBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @OneToMany(mappedBy = "parentAccount", cascade = CascadeType.ALL)
    @OrderBy("code ASC")
    @Builder.Default
    private List<Account> childAccounts = new ArrayList<>();

    @Column(name = "account_level", nullable = false)
    @Builder.Default
    private Integer accountLevel = 1;

    @Column(name = "full_path", length = 200)
    private String fullPath;

    @Column(name = "is_system_account", nullable = false)
    @Builder.Default
    private boolean systemAccount = false;

    @Column(name = "is_control_account", nullable = false)
    @Builder.Default
    private boolean controlAccount = false;

    @Column(name = "allows_direct_posting", nullable = false)
    @Builder.Default
    private boolean allowsDirectPosting = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "current_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "ytd_debit", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal ytdDebit = BigDecimal.ZERO;

    @Column(name = "ytd_credit", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal ytdCredit = BigDecimal.ZERO;

    @Column(name = "opening_balance", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 1000)
    private String notes;

    /**
     * Adds a child account and sets this as its parent.
     */
    public void addChildAccount(Account child) {
        childAccounts.add(child);
        child.setParentAccount(this);
        child.setAccountLevel(this.accountLevel + 1);
        updateChildPath(child);
    }

    /**
     * Removes a child account.
     */
    public void removeChildAccount(Account child) {
        childAccounts.remove(child);
        child.setParentAccount(null);
    }

    /**
     * Updates the full path for a child account.
     */
    private void updateChildPath(Account child) {
        if (this.fullPath != null && !this.fullPath.isEmpty()) {
            child.setFullPath(this.fullPath + "/" + child.getCode());
        } else {
            child.setFullPath(this.code + "/" + child.getCode());
        }
    }

    /**
     * Checks if this account is a leaf node (has no children).
     */
    public boolean isLeafAccount() {
        return childAccounts == null || childAccounts.isEmpty();
    }

    /**
     * Checks if this account is a root account (has no parent).
     */
    public boolean isRootAccount() {
        return parentAccount == null;
    }

    /**
     * Gets the debit balance if normal balance is debit, otherwise credit balance.
     */
    public BigDecimal getBalance() {
        BigDecimal netAmount = ytdDebit.subtract(ytdCredit).add(openingBalance);
        if (normalBalance == NormalBalance.CREDIT) {
            return netAmount.negate();
        }
        return netAmount;
    }

    /**
     * Determines the normal balance based on account type.
     */
    public static NormalBalance determineNormalBalance(AccountType accountType) {
        return switch (accountType) {
            case ASSET, EXPENSE -> NormalBalance.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> NormalBalance.CREDIT;
        };
    }
}
