package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class ApplyDiscountRequest {

    /**
     * Fixed discount amount
     */
    @DecimalMin(value = "0", message = "Discount amount must be non-negative")
    private BigDecimal amount;

    /**
     * Percentage discount (0-100)
     */
    @DecimalMin(value = "0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100", message = "Discount percent must not exceed 100")
    private BigDecimal percent;

    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;
}
