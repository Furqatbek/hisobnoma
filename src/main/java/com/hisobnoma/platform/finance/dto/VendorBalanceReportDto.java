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
public class VendorBalanceReportDto {
    private LocalDate reportDate;
    private Long tenantId;

    // Summary
    private BigDecimal totalPayable;
    private BigDecimal totalPayments;
    private BigDecimal netBalance;
    private int vendorCount;

    // Vendor balances
    private List<VendorBalanceDto> vendorBalances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorBalanceDto {
        private Long vendorId;
        private String vendorCode;
        private String vendorName;
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
    }
}
