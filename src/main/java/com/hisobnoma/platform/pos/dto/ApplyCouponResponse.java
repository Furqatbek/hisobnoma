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
public class ApplyCouponResponse {
    private boolean valid;
    private String couponCode;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal newTotal;
    private String promotionCode;
    private String promotionName;
    private String promotionDescription;
}
