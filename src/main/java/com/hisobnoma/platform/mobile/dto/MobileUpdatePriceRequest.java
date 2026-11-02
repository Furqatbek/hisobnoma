package com.hisobnoma.platform.mobile.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Mobile request to change a product's price. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileUpdatePriceRequest {

    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;

    /** Optional — only updated when provided. */
    private BigDecimal wholesalePrice;
}
