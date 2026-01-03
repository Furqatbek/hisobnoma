package com.hisobnoma.platform.inventory.dto;

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
public class ReceivingLineDto {

    private Long id;
    private Long receivingOrderId;
    private Long poLineId;
    private Long productId;
    private String productSku;
    private String productName;
    private Long productVariantId;
    private String variantName;
    private Integer lineNumber;
    private String description;
    private BigDecimal expectedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal acceptedQuantity;
    private BigDecimal rejectedQuantity;
    private String rejectionReason;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private BigDecimal unitCost;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private String batchNumber;
    private String serialNumbers;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private Long targetLocationId;
    private String targetLocationName;
    private String notes;
    private Long stockMovementId;
    private BigDecimal varianceQuantity;
    private boolean hasVariance;
    private boolean overReceived;
    private boolean underReceived;
}
