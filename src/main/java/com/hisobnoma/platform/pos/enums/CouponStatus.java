package com.hisobnoma.platform.pos.enums;

/**
 * Status of a coupon.
 */
public enum CouponStatus {
    ACTIVE,     // Coupon is active and can be used
    INACTIVE,   // Coupon is inactive (not yet started or paused)
    EXPIRED,    // Coupon has expired
    DEPLETED,   // All uses have been exhausted
    CANCELLED   // Coupon has been cancelled
}
