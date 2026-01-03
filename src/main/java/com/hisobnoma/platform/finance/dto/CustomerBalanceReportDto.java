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

    // Summary
    private BigDecimal totalReceivable;
    private BigDecimal totalReceived;
    private BigDecimal netBalance;
    private int customerCount;

    // Customer balances
    private List<CustomerBalanceDto> customerBalances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerBalanceDto {
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String contactPerson;
        private String email;
        private String phone;
        private BigDecimal creditLimit;
        private BigDecimal totalInvoiced;
        private BigDecimal totalPaid;
        private BigDecimal currentBalance;
        private BigDecimal availableCredit;
        private int openInvoiceCount;
        private LocalDate lastInvoiceDate;
        private LocalDate lastPaymentDate;
        private boolean creditHold;
        private boolean overCreditLimit;
    }
}
