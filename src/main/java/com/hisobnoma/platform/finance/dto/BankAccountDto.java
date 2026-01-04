package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.BankAccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto {
    private Long id;
    private String accountCode;
    private String accountName;
    private String description;
    private BankAccountType accountType;
    private String bankName;
    private String bankBranch;
    private String bankCode;
    private String accountNumber;
    private String iban;
    private String swiftCode;
    private String currency;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private BigDecimal openingBalance;
    private LocalDate openingBalanceDate;
    private LocalDate lastReconciledDate;
    private BigDecimal lastReconciledBalance;
    private Long glAccountId;
    private String glAccountCode;
    private String glAccountName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private boolean active;
    private boolean defaultAccount;
    private boolean allowPayments;
    private boolean allowReceipts;
    private Integer sortOrder;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
