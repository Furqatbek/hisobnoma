package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.TargetPeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class CreateDistributionAgentTargetRequest {

    @NotNull(message = "Agent is required")
    private Long agentId;

    private TargetPeriodType periodType;

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

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
