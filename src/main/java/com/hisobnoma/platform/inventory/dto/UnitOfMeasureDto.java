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
public class UnitOfMeasureDto {
    private Long id;
    private String code;
    private String name;
    private String symbol;
    private String description;
    private Long baseUomId;
    private String baseUomCode;
    private String baseUomName;
    private BigDecimal conversionFactor;
    private boolean active;
    private boolean isBaseUnit;
}
