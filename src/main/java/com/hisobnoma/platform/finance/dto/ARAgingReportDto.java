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
public class ARAgingReportDto {
    private LocalDate reportDate;
    private Long tenantId;

    // Customer breakdown
    private List<CustomerAgingDto> customerAgingList;

    // Summary totals
    private BigDecimal totalCurrent;
    private BigDecimal total1To30Days;
    private BigDecimal total31To60Days;
    private BigDecimal total61To90Days;
    private BigDecimal totalOver90Days;
    private BigDecimal grandTotal;
    private int customerCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerAgingDto {
        private Long customerId;
        private String customerCode;
        private String customerName;
        private BigDecimal currentAmount;
        private BigDecimal days1To30;
        private BigDecimal days31To60;
        private BigDecimal days61To90;
        private BigDecimal over90Days;
        private BigDecimal totalBalance;
        private BigDecimal creditLimit;
        private BigDecimal availableCredit;
    }
}
