package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VanLoadoutLineDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String unitName;
    private BigDecimal quantityLoaded;
    private BigDecimal quantityReturned;
    private BigDecimal quantityDamaged;
    private BigDecimal quantitySold;
    private BigDecimal unitCost;
    private BigDecimal unitPrice;
}
