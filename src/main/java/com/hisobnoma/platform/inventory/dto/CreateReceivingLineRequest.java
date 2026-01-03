package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class CreateReceivingLineRequest {

    private Long poLineId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long productVariantId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private BigDecimal expectedQuantity;

    @NotNull(message = "Received quantity is required")
    @Positive(message = "Received quantity must be positive")
    private BigDecimal receivedQuantity;

    private BigDecimal rejectedQuantity;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    private String rejectionReason;

    @NotNull(message = "Unit of measure ID is required")
    private Long uomId;

    @NotNull(message = "Unit cost is required")
    @Positive(message = "Unit cost must be positive")
    private BigDecimal unitCost;

    private BigDecimal taxPercent;

    @Size(max = 100, message = "Batch number must not exceed 100 characters")
    private String batchNumber;

    @Size(max = 2000, message = "Serial numbers must not exceed 2000 characters")
    private String serialNumbers;

    private LocalDate manufactureDate;
    private LocalDate expiryDate;

    private Long targetLocationId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
