package com.hisobnoma.platform.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionActionDto {
    private Long id;
    private Long promotionId;
    private String actionType;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal setPrice;
    private BigDecimal maxDiscount;
    private Long freeProductId;
    private String freeProductName;
    private Integer freeQuantity;
    private String targetProductIds;
    private String targetCategoryIds;
    private String applyTo;
    private Integer applyCount;
    private Integer sortOrder;
    private String notes;
}
