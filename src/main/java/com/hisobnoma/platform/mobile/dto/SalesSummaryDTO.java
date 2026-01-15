package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sales summary DTO for mobile dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryDTO {

    private List<RecentTransaction> recentTransactions;
    private List<SalesByLocation> salesByLocation;
    private List<SalesByCategory> salesByCategory;
    private List<TopProduct> topProducts;
    private List<TopCustomer> topCustomers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentTransaction {
        private Long id;
        private String transactionNumber;
        private String transactionType;
        private String customerName;
        private BigDecimal totalAmount;
        private String paymentMethod;
        private String status;
        private Instant completedAt;
        private String locationName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesByLocation {
        private Long locationId;
        private String locationName;
        private BigDecimal totalSales;
        private Integer transactionCount;
        private BigDecimal percentOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesByCategory {
        private Long categoryId;
        private String categoryName;
        private BigDecimal totalSales;
        private Integer itemsSold;
        private BigDecimal percentOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProduct {
        private Long productId;
        private String productName;
        private String sku;
        private BigDecimal totalSales;
        private Integer quantitySold;
        private Integer rank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCustomer {
        private Long customerId;
        private String customerName;
        private BigDecimal totalPurchases;
        private Integer transactionCount;
        private Integer rank;
    }
}
