package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAPPaymentAllocationRequest {

    @NotNull(message = "Invoice ID is required")
    private Long apInvoiceId;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.01", message = "Allocated amount must be positive")
    private BigDecimal allocatedAmount;

    @DecimalMin(value = "0.0", message = "Discount taken must be non-negative")
    private BigDecimal discountTaken;

    @DecimalMin(value = "0.0", message = "Write-off amount must be non-negative")
    private BigDecimal writeOffAmount;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
