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
public class TaxReportDto {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String taxType;
    private BigDecimal totalSales;
    private BigDecimal totalSalesTax;
    private BigDecimal totalPurchases;
    private BigDecimal totalPurchaseTax;
    private BigDecimal netTaxPayable;
    private List<TaxReportLineDto> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxReportLineDto {
        private String taxCode;
        private String taxName;
        private BigDecimal rate;
        private BigDecimal salesAmount;
        private BigDecimal salesTax;
        private BigDecimal purchaseAmount;
        private BigDecimal purchaseTax;
        private BigDecimal netTax;
    }
}
