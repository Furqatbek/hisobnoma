package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.ReconciliationStatus;
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
public class BankReconciliationDto {
    private Long id;
    private Long bankAccountId;
    private String bankAccountCode;
    private String bankAccountName;
    private String reconciliationNumber;
    private LocalDate statementDate;
    private LocalDate statementStartDate;
    private LocalDate statementEndDate;
    private ReconciliationStatus status;
    private BigDecimal openingBalance;
    private BigDecimal statementEndingBalance;
    private BigDecimal bookBalance;
    private BigDecimal clearedDeposits;
    private BigDecimal clearedWithdrawals;
    private BigDecimal outstandingDeposits;
    private BigDecimal outstandingWithdrawals;
    private BigDecimal adjustedBankBalance;
    private BigDecimal adjustedBookBalance;
    private BigDecimal difference;
    private Integer transactionsCleared;
    private Integer transactionsOutstanding;
    private Instant completedAt;
    private Long completedBy;
    private Instant cancelledAt;
    private Long cancelledBy;
    private String cancellationReason;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
