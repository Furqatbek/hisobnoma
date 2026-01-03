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
public class APAgingReportDto {
    private LocalDate reportDate;
    private Long tenantId;

    // Summary totals
    private BigDecimal totalPayable;
    private BigDecimal current;          // Not yet due
    private BigDecimal overdue1To30;     // 1-30 days overdue
    private BigDecimal overdue31To60;    // 31-60 days overdue
    private BigDecimal overdue61To90;    // 61-90 days overdue
    private BigDecimal overdueOver90;    // >90 days overdue

    // Vendor breakdown
    private List<VendorAgingDto> vendorAging;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorAgingDto {
        private Long vendorId;
        private String vendorCode;
        private String vendorName;
        private BigDecimal totalBalance;
        private BigDecimal current;
        private BigDecimal overdue1To30;
        private BigDecimal overdue31To60;
        private BigDecimal overdue61To90;
        private BigDecimal overdueOver90;
        private int invoiceCount;
    }
}
