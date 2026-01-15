package com.hisobnoma.platform.reports.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceReportDTO {
    private ReportMetadata metadata;
    private List<AccountBalance> accounts;
    private Totals totals;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String reportName;
        private Instant generatedAt;
        private LocalDate asOfDate;
        private String fiscalYear;
        private String period;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountBalance {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private String accountType;
        private String accountSubType;
        private BigDecimal debitBalance;
        private BigDecimal creditBalance;
        private int level;
        private boolean isHeader;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private boolean isBalanced;
        private BigDecimal difference;
    }
}
