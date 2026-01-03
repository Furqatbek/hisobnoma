package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private AccountType accountType;
    private NormalBalance normalBalance;
    private Long parentAccountId;
    private String parentAccountCode;
    private String parentAccountName;
    private Integer accountLevel;
    private String fullPath;
    private boolean systemAccount;
    private boolean controlAccount;
    private boolean allowsDirectPosting;
    private boolean active;
    private BigDecimal currentBalance;
    private BigDecimal ytdDebit;
    private BigDecimal ytdCredit;
    private BigDecimal openingBalance;
    private String currency;
    private String taxCode;
    private String bankAccountNumber;
    private String bankName;
    private Integer sortOrder;
    private String notes;
    private List<AccountDto> childAccounts;
    private Instant createdAt;
    private Instant updatedAt;
}
