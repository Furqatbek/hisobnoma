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
public class CreatePurchaseOrderLineRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long productVariantId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Unit of measure ID is required")
    private Long uomId;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    private BigDecimal discountPercent;
    private BigDecimal taxPercent;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    private LocalDate expectedDate;
}
