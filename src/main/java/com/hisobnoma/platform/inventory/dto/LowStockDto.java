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
public class LowStockDto {

    private Long productId;
    private String productSku;
    private String productName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityAvailable;
    private BigDecimal reorderPoint;
    private BigDecimal reorderQuantity;
    private BigDecimal minStockLevel;
    private BigDecimal shortfallQuantity;
    private String severity; // CRITICAL, LOW, WARNING
    private String categoryName;
    private String brandName;
}
