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
public class ProductVariantDto {
    private Long id;
    private Long productId;
    private String sku;
    private String barcode;
    private String name;
    private String fullName;

    private String option1Name;
    private String option1Value;
    private String option2Name;
    private String option2Value;
    private String option3Name;
    private String option3Value;

    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal priceDifference;
    private BigDecimal effectiveSellingPrice;
    private BigDecimal effectiveCostPrice;

    private BigDecimal weight;
    private String imageUrl;
    private Integer sortOrder;
    private boolean active;
}
