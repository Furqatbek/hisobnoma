package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A requested order line. Only {@code productId}, {@code quantity} and an optional
 * {@code discountPercent} are honoured — the unit price is resolved server-side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionOrderLineRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be positive")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "Discount must be non-negative")
    @DecimalMax(value = "100", message = "Discount must not exceed 100")
    private BigDecimal discountPercent;
}
