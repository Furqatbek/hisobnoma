package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Staff request to issue personal coupon(s): the discount comes from the referenced promotion. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCouponRequest {

    @NotNull(message = "promotionId is required")
    private Long promotionId;

    /** Coupon validity from today; default 30 days. */
    private Integer validityDays;

    /** Optional note stored as the coupon description. */
    private String note;

    /** Send the code to the customer by SMS (needs platform SMS enabled). */
    private Boolean sendSms;
}
