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
public class SalesSummaryReportDTO {
    private ReportMetadata metadata;
    private Summary summary;
    private List<DailySales> dailyBreakdown;
    private List<CategorySales> byCategory;
    private List<ProductSales> topProducts;
    private List<PaymentMethodSales> byPaymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String reportName;
        private Instant generatedAt;
        private LocalDate startDate;
        private LocalDate endDate;
        private String locationFilter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal grossSales;
        private BigDecimal discounts;
        private BigDecimal returns;
        private BigDecimal netSales;
        private BigDecimal costOfGoods;
        private BigDecimal grossProfit;
        private BigDecimal grossMarginPercent;
        private int transactionCount;
        private BigDecimal averageTransactionValue;
        private int itemsSold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySales {
        private LocalDate date;
        private String dayOfWeek;
        private BigDecimal netSales;
        private int transactionCount;
        private BigDecimal averageTransaction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySales {
        private Long categoryId;
        private String categoryName;
        private BigDecimal netSales;
        private int itemsSold;
        private BigDecimal percentOfTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSales {
        private Long productId;
        private String sku;
        private String productName;
        private int quantitySold;
        private BigDecimal netSales;
        private BigDecimal profit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodSales {
        private String paymentMethod;
        private BigDecimal amount;
        private int transactionCount;
        private BigDecimal percentOfTotal;
    }
}
