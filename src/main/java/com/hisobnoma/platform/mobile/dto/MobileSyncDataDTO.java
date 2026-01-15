package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sync data DTO for mobile offline support.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileSyncDataDTO {

    private Instant lastSyncAt;
    private String syncVersion;
    private boolean fullSyncRequired;

    private List<ProductSync> products;
    private List<CustomerSync> customers;
    private List<CategorySync> categories;
    private List<PriceSync> prices;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSync {
        private Long id;
        private String sku;
        private String barcode;
        private String name;
        private Long categoryId;
        private String categoryName;
        private BigDecimal sellingPrice;
        private BigDecimal costPrice;
        private String unitOfMeasure;
        private boolean trackInventory;
        private boolean active;
        private String imageUrl;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerSync {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private String email;
        private Long priceListId;
        private BigDecimal creditLimit;
        private BigDecimal currentBalance;
        private boolean active;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySync {
        private Long id;
        private String name;
        private Long parentId;
        private Integer sortOrder;
        private boolean active;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceSync {
        private Long productId;
        private Long priceListId;
        private String priceListName;
        private BigDecimal unitPrice;
        private BigDecimal minQuantity;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private Instant updatedAt;
    }
}
