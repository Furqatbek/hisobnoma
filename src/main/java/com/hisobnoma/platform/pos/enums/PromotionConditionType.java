package com.hisobnoma.platform.pos.enums;

/**
 * Types of conditions that trigger a promotion.
 */
public enum PromotionConditionType {
    MINIMUM_PURCHASE,    // Minimum purchase amount required
    MINIMUM_QUANTITY,    // Minimum quantity of items
    SPECIFIC_PRODUCTS,   // Specific products must be in cart
    CATEGORY,            // Products from specific category
    BRAND,               // Products from specific brand
    CUSTOMER_GROUP,      // Customer must belong to group
    FIRST_PURCHASE,      // First purchase by customer
    TIME_BASED,          // Active during specific hours
    DAY_OF_WEEK,         // Active on specific days
    PAYMENT_METHOD       // Specific payment method used
}
