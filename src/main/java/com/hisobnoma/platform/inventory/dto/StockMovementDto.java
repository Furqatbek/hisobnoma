package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDto {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long productVariantId;
    private String variantName;
    private Long fromLocationId;
    private String fromLocationName;
    private Long toLocationId;
    private String toLocationName;
    private MovementType movementType;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private MovementReferenceType referenceType;
    private Long referenceId;
    private String referenceNumber;
    private LocalDateTime movementDate;
    private String reason;
    private String notes;
    private String batchNumber;
    private String serialNumber;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private boolean reversed;
    private Long reversalMovementId;
    private Long originalMovementId;
    private LocalDateTime createdAt;
    private Long createdBy;
}
