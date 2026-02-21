package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUomDto {

    private Long id;
    private Long productId;
    private String productName;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private String uomSymbol;
    private BigDecimal conversionFactor;
    private BigDecimal sellingPrice;
    private BigDecimal effectiveSellingPrice;
    private boolean defaultSale;
    private boolean active;
    private Integer sortOrder;
}
