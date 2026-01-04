package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private PromotionType type;
    private PromotionScope scope;
    private Integer priority;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private Integer buyQuantity;
    private Integer getQuantity;
    private BigDecimal getDiscountPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String daysOfWeek;
    private boolean active;
    private boolean stackable;
    private boolean requiresCoupon;
    private Integer maxUses;
    private Integer currentUses;
    private Integer maxUsesPerCustomer;
    private BigDecimal minOrderAmount;
    private Long locationId;
    private String locationName;
    private String notes;
    private List<PromotionConditionDto> conditions;
    private List<PromotionActionDto> actions;
    private Integer couponCount;
}
