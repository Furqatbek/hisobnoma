package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLineDto {
    private Long id;
    private Long journalEntryId;
    private Integer lineNumber;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal baseDebitAmount;
    private BigDecimal baseCreditAmount;
    private String costCenter;
    private String projectCode;
    private String department;
    private String referenceType;
    private Long referenceId;
    private String taxCode;
    private BigDecimal taxAmount;
}
