package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.NotNull;
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
public class CreatePriceListItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long variantId;
    private BigDecimal price;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal markupPercent;
    private BigDecimal minQuantity;
    private BigDecimal maxQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}
