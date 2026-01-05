package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

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
    private BigDecimal currentStock;
    private BigDecimal quantityAvailable;
    private BigDecimal reorderPoint;
    private BigDecimal reorderQuantity;
    private BigDecimal minStockLevel;
    private BigDecimal shortfallQuantity;
    private BigDecimal stockValue;
    private String severity; // CRITICAL, LOW, WARNING
    private String categoryName;
    private String brandName;

    // Slow moving / Dead stock fields
    private Integer daysSinceLastSale;
    private Instant lastSaleDate;
    private String recommendation; // DISCOUNT, WRITE_OFF
}
