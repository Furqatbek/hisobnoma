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
public class AbcAnalysisDto {

    private Long productId;
    private String productSku;
    private String productName;
    private String categoryName;
    private String classification; // A, B, C
    private BigDecimal annualValue;
    private BigDecimal percentageOfTotal;
    private BigDecimal cumulativePercentage;
    private BigDecimal currentStock;
    private BigDecimal stockValue;
    private Integer salesCount;
    private BigDecimal averageUnitCost;
    private Integer rank;
}
