package com.hisobnoma.platform.mobile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for quick stock count from mobile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickStockCountRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long variantId;

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @NotNull(message = "Counted quantity is required")
    @Positive(message = "Counted quantity must be positive")
    private BigDecimal countedQuantity;

    private String notes;
}
