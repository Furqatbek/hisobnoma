package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockValuationDto {

    private BigDecimal totalValue;
    private BigDecimal totalQuantity;
    private Long totalProducts;
    private Long totalLocations;
    private List<LocationValuation> byLocation;
    private List<CategoryValuation> byCategory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationValuation {
        private Long locationId;
        private String locationCode;
        private String locationName;
        private BigDecimal totalValue;
        private BigDecimal totalQuantity;
        private Long productCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryValuation {
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private BigDecimal totalValue;
        private BigDecimal totalQuantity;
        private Long productCount;
    }
}
