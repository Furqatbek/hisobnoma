package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateUomRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    private Long baseUomId;

    @Positive(message = "Conversion factor must be positive")
    @Builder.Default
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean isBaseUnit = true;
}
