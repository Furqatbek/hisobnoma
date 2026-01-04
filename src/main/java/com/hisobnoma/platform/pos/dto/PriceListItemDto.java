package com.hisobnoma.platform.pos.dto;

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
public class PriceListItemDto {
    private Long id;
    private Long priceListId;
    private Long productId;
    private String productCode;
    private String productName;
    private Long variantId;
    private String variantName;
    private BigDecimal price;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal markupPercent;
    private BigDecimal minQuantity;
    private BigDecimal maxQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private String notes;
    private BigDecimal basePrice;
    private BigDecimal effectivePrice;
}
