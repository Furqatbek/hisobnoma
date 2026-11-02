package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** The product's price fields after a mobile price change. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileProductPriceDto {
    private Long productId;
    private String name;
    private BigDecimal sellingPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal minSellingPrice;
}
