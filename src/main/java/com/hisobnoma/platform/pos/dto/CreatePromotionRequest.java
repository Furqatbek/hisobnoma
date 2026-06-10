package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreatePromotionRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Type is required")
    private PromotionType type;

    private PromotionScope scope;
    private PromotionChannel channel;
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
    private boolean stackable;
    private boolean requiresCoupon;
    private Integer maxUses;
    private Integer maxUsesPerCustomer;
    private BigDecimal minOrderAmount;
    private Long locationId;
    private String notes;
    private List<CreatePromotionConditionRequest> conditions;
    private List<CreatePromotionActionRequest> actions;
}
