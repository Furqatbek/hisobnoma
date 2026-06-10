package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Coupon validation outcome shown to the customer. Deliberately generic —
 * the response never distinguishes unknown, expired, depleted or
 * wrong-channel codes, so the endpoint can't be used to probe coupons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCouponValidationDto {

    private String couponCode;
    private boolean valid;

    /**
     * Discount the coupon would apply to this cart (zero when invalid).
     */
    private BigDecimal discount;
}
