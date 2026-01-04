package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.PromotionConditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionConditionDto {
    private Long id;
    private Long promotionId;
    private PromotionConditionType conditionType;
    private String operator;
    private String value;
    private String value2;
    private BigDecimal thresholdAmount;
    private String productIds;
    private String categoryIds;
    private String brandIds;
    private String customerGroups;
    private boolean required;
    private String notes;
}
