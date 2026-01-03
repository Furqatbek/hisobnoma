package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBalanceReportDto {
    private LocalDate reportDate;
    private Long tenantId;

    // Customer balances
    private List<CustomerBalanceDto> customerBalances;

    // Summary
    private BigDecimal totalReceivables;
    private BigDecimal totalCredits;
    private BigDecimal totalNetBalance;
    private int customerCount;
    private int customersOverCreditLimit;
    private int customersOnCreditHold;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerBalanceDto {
        private Long customerId;
        private String customerCode;
        private String customerName;
        private BigDecimal outstandingInvoices;
        private BigDecimal availableCredits;
        private BigDecimal netBalance;
        private BigDecimal creditLimit;
        private BigDecimal availableCreditLimit;
        private boolean overCreditLimit;
        private boolean onCreditHold;
        private LocalDate lastInvoiceDate;
        private LocalDate lastPaymentDate;
        private Integer paymentTerms;
    }
}
