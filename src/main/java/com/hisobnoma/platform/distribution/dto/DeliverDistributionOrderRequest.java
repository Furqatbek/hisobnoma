package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for marking an order delivered. {@code cashCollected} records money taken
 * on the doorstep (for CASH / MIXED orders); the remainder becomes the AR credit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverDistributionOrderRequest {

    @DecimalMin(value = "0", message = "Cash collected must be non-negative")
    private BigDecimal cashCollected;
}
