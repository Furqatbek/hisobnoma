package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.VisitOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitCheckOutRequest {

    @NotNull(message = "Outcome is required")
    private VisitOutcome outcome;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private Long distributionOrderId;

    /**
     * Cash collected from the customer against outstanding AR invoices.
     * Creates a completed (GL-posted) AR payment, allocated oldest-due-first
     * across the customer's open invoices; any excess stays unallocated
     * (customer advance).
     */
    @Positive(message = "collectedAmount must be positive")
    private BigDecimal collectedAmount;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
