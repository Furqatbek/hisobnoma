package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.BankTransactionStatus;
import com.hisobnoma.platform.finance.entity.BankTransactionType;
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
public class BankTransactionDto {
    private Long id;
    private Long bankAccountId;
    private String bankAccountCode;
    private String bankAccountName;
    private String transactionNumber;
    private LocalDate transactionDate;
    private LocalDate valueDate;
    private BankTransactionType transactionType;
    private BankTransactionStatus status;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal runningBalance;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal baseAmount;
    private String referenceNumber;
    private String checkNumber;
    private String payeePayer;
    private String referenceType;
    private Long referenceId;
    private Long counterAccountId;
    private String counterAccountCode;
    private String counterAccountName;
    private Long transferToAccountId;
    private String transferToAccountCode;
    private String transferToAccountName;
    private boolean glPosted;
    private Long glJournalEntryId;
    private Long reconciliationId;
    private LocalDate reconciledDate;
    private String bankStatementLine;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
