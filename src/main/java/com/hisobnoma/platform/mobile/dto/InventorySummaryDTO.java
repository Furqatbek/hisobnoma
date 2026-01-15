package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Inventory summary DTO for mobile dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySummaryDTO {

    private Integer totalSkuCount;
    private Integer activeSkuCount;
    private BigDecimal totalInventoryValue;
    private Integer lowStockCount;
    private Integer outOfStockCount;
    private Integer expiringCount;
    private Integer reorderPendingCount;

    private List<LowStockItem> lowStockItems;
    private List<OutOfStockItem> outOfStockItems;
    private List<ExpiringItem> expiringItems;
    private List<RecentMovement> recentMovements;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockItem {
        private Long productId;
        private String productName;
        private String sku;
        private BigDecimal currentStock;
        private BigDecimal reorderPoint;
        private BigDecimal minStockLevel;
        private String locationName;
        private String urgency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OutOfStockItem {
        private Long productId;
        private String productName;
        private String sku;
        private String locationName;
        private LocalDate lastStockDate;
        private Integer daysSinceOutOfStock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpiringItem {
        private Long productId;
        private String productName;
        private String sku;
        private String batchNumber;
        private BigDecimal quantity;
        private LocalDate expiryDate;
        private Integer daysUntilExpiry;
        private String locationName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentMovement {
        private Long movementId;
        private String movementType;
        private String productName;
        private String sku;
        private BigDecimal quantity;
        private String fromLocation;
        private String toLocation;
        private String reference;
        private String createdBy;
        private java.time.Instant createdAt;
    }
}
