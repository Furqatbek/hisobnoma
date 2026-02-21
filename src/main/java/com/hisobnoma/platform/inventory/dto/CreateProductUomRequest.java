package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductUomRequest {

    @NotNull(message = "UOM ID is required")
    private Long uomId;

    @NotNull(message = "Conversion factor is required")
    @DecimalMin(value = "0.000001", message = "Conversion factor must be greater than 0")
    private BigDecimal conversionFactor;

    private BigDecimal sellingPrice;

    @Builder.Default
    private boolean defaultSale = false;

    @Builder.Default
    private boolean active = true;

    private Integer sortOrder;
}
