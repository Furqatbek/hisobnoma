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
public class UpdateLineRequest {

    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    private BigDecimal unitPrice;

    @DecimalMin(value = "0", message = "Discount amount must be non-negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100", message = "Discount percent must not exceed 100")
    private BigDecimal discountPercent;

    @Size(max = 200, message = "Discount reason must not exceed 200 characters")
    private String discountReason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
