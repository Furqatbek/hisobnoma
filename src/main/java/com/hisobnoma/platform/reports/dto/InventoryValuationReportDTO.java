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
public class InventoryValuationReportDTO {
    private ReportMetadata metadata;
    private Summary summary;
    private List<ValuationItem> items;
    private List<CategorySummary> byCategory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String reportName;
        private Instant generatedAt;
        private LocalDate asOfDate;
        private String valuationMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int totalSkus;
        private BigDecimal totalQuantity;
        private BigDecimal totalCostValue;
        private BigDecimal totalRetailValue;
        private BigDecimal potentialMargin;
        private BigDecimal marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValuationItem {
        private Long productId;
        private String sku;
        private String productName;
        private String category;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private BigDecimal unitRetail;
        private BigDecimal totalRetail;
        private BigDecimal margin;
        private BigDecimal marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long categoryId;
        private String categoryName;
        private int skuCount;
        private BigDecimal totalQuantity;
        private BigDecimal totalCostValue;
        private BigDecimal totalRetailValue;
        private BigDecimal percentOfTotal;
    }
}
