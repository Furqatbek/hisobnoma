package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Financial summary DTO for mobile dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialSummaryDTO {

    private BigDecimal totalCashBalance;
    private BigDecimal totalBankBalance;
    private BigDecimal arOutstanding;
    private BigDecimal apOutstanding;
    private BigDecimal netCashPosition;

    private BigDecimal todayIncome;
    private BigDecimal todayExpenses;
    private BigDecimal todayNetProfit;

    private BigDecimal thisMonthIncome;
    private BigDecimal thisMonthExpenses;
    private BigDecimal thisMonthNetProfit;
    private BigDecimal profitMarginPercent;

    private CashFlowSummary cashFlow;
    private ReceivablesSummary receivables;
    private PayablesSummary payables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CashFlowSummary {
        private BigDecimal openingBalance;
        private BigDecimal totalInflows;
        private BigDecimal totalOutflows;
        private BigDecimal closingBalance;
        private List<CashFlowItem> recentFlows;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CashFlowItem {
        private String description;
        private BigDecimal amount;
        private String type;
        private java.time.Instant date;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceivablesSummary {
        private BigDecimal totalOutstanding;
        private BigDecimal current;
        private BigDecimal overdue1To30;
        private BigDecimal overdue31To60;
        private BigDecimal overdue61To90;
        private BigDecimal overdueOver90;
        private Integer totalInvoices;
        private Integer overdueInvoices;
        private List<OverdueInvoice> topOverdueInvoices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayablesSummary {
        private BigDecimal totalOutstanding;
        private BigDecimal current;
        private BigDecimal dueSoon;
        private BigDecimal overdue;
        private Integer totalInvoices;
        private Integer dueSoonCount;
        private List<DueInvoice> upcomingPayments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverdueInvoice {
        private Long invoiceId;
        private String invoiceNumber;
        private String customerName;
        private BigDecimal amount;
        private LocalDate dueDate;
        private Integer daysOverdue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DueInvoice {
        private Long invoiceId;
        private String invoiceNumber;
        private String vendorName;
        private BigDecimal amount;
        private LocalDate dueDate;
        private Integer daysUntilDue;
    }
}
