package com.hisobnoma.platform.reports.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOnHandReportDTO {
    private ReportMetadata metadata;
    private Summary summary;
    private List<StockItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String reportName;
        private Instant generatedAt;
        private String generatedBy;
        private String locationFilter;
        private String categoryFilter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int totalSkus;
        private BigDecimal totalQuantity;
        private BigDecimal totalValue;
        private int lowStockCount;
        private int outOfStockCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private Long productId;
        private String sku;
        private String productName;
        private String category;
        private String location;
        private String uom;
        private BigDecimal quantityOnHand;
        private BigDecimal quantityReserved;
        private BigDecimal quantityAvailable;
        private BigDecimal unitCost;
        private BigDecimal totalValue;
        private BigDecimal reorderPoint;
        private String stockStatus;
    }
}
