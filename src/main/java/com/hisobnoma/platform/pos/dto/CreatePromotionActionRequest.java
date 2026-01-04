package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromotionActionRequest {

    @NotBlank(message = "Action type is required")
    private String actionType;

    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal setPrice;
    private BigDecimal maxDiscount;
    private Long freeProductId;
    private Integer freeQuantity;
    private String targetProductIds;
    private String targetCategoryIds;
    private String applyTo;
    private Integer applyCount;
    private Integer sortOrder;
    private String notes;
}
