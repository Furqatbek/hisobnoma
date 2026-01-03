package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSuggestionDto {

    private Long productId;
    private String productSku;
    private String productName;
    private String categoryName;
    private BigDecimal currentStock;
    private BigDecimal reorderPoint;
    private BigDecimal reorderQuantity;
    private BigDecimal suggestedOrderQuantity;
    private BigDecimal averageDailySales;
    private Integer daysOfStock;
    private BigDecimal lastCost;
    private BigDecimal estimatedOrderValue;
    private Long preferredVendorId;
    private String preferredVendorName;
    private Integer vendorLeadTimeDays;
    private String priority; // HIGH, MEDIUM, LOW
}
