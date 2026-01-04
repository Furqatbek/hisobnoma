package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.PromotionConditionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromotionConditionRequest {

    @NotNull(message = "Condition type is required")
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
