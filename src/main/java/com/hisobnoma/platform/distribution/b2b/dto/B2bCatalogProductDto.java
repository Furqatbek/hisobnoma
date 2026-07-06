package com.hisobnoma.platform.distribution.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B2bCatalogProductDto {
    private Long productId;
    private String sku;
    private String name;
    private String unitName;
    /** Unit price resolved against the buyer's price list. */
    private BigDecimal price;
    private String currency;
}
