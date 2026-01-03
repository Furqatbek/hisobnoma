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
public class InventoryCountLineDto {

    private Long id;
    private Long inventoryCountId;
    private Long productId;
    private String productSku;
    private String productName;
    private Long productVariantId;
    private String variantName;
    private Integer lineNumber;
    private BigDecimal systemQuantity;
    private BigDecimal countedQuantity;
    private BigDecimal varianceQuantity;
    private BigDecimal unitCost;
    private BigDecimal varianceValue;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private String batchNumber;
    private String serialNumber;
    private boolean counted;
    private Long countedBy;
    private Instant countedAt;
    private boolean recountRequired;
    private BigDecimal recountQuantity;
    private Long recountedBy;
    private Instant recountedAt;
    private String notes;
    private String adjustmentReason;
    private Long stockMovementId;
    private boolean adjustmentApproved;
    private boolean hasVariance;
    private boolean positiveVariance;
    private boolean negativeVariance;
    private BigDecimal variancePercentage;
}
