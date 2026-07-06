package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Updates a target's goal values. The agent and period are fixed once created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDistributionAgentTargetRequest {

    @DecimalMin(value = "0", message = "Target revenue must be non-negative")
    private BigDecimal targetRevenue;

    @Min(value = 0, message = "Target orders must be non-negative")
    private Integer targetOrders;

    @Min(value = 0, message = "Target visits must be non-negative")
    private Integer targetVisits;

    @Min(value = 0, message = "Target new customers must be non-negative")
    private Integer targetNewCustomers;

    @DecimalMin(value = "0", message = "Target collection must be non-negative")
    private BigDecimal targetCollection;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
