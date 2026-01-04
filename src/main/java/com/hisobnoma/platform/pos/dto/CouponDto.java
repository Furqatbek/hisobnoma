package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    private Long id;
    private String code;
    private Long promotionId;
    private String promotionCode;
    private String promotionName;
    private CouponStatus status;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxUses;
    private Integer currentUses;
    private Integer maxUsesPerCustomer;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Instant firstUsedAt;
    private Instant lastUsedAt;
    private String notes;
    private Integer redemptionCount;
    private Integer remainingUses;
    private java.math.BigDecimal minimumOrderAmount;
    private Long restrictedToCustomerId;
}
