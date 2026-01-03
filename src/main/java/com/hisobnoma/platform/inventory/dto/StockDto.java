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
public class StockDto {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String productBarcode;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal quantityAvailable;
    private BigDecimal quantityInTransit;
    private BigDecimal quantityOnOrder;
    private BigDecimal averageCost;
    private BigDecimal lastCost;
    private BigDecimal stockValue;
    private BigDecimal reorderPoint;
    private BigDecimal reorderQuantity;
    private BigDecimal minStockLevel;
    private BigDecimal maxStockLevel;
    private boolean belowReorderPoint;
    private boolean belowMinimum;
    private String baseUomCode;
    private String baseUomName;
}
