package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.StockBatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchDto {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private String batchNumber;
    private BigDecimal quantity;
    private BigDecimal initialQuantity;
    private BigDecimal quantityReserved;
    private BigDecimal quantityAvailable;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private LocalDate receivedDate;
    private String supplierBatchNumber;
    private BigDecimal unitCost;
    private BigDecimal value;
    private StockBatch.BatchStatus status;
    private String notes;
    private Long supplierId;
    private Long receivingOrderId;
    private Long daysUntilExpiry;
    private boolean expired;
    private boolean expiringWithin30Days;
}
