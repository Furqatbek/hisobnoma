package com.hisobnoma.platform.pos.enums;

/**
 * Types of promotions.
 */
public enum PromotionType {
    PERCENTAGE_OFF,     // X% off
    FIXED_AMOUNT_OFF,   // $X off
    BUY_X_GET_Y,        // Buy X get Y free/discounted
    BUNDLE,             // Bundle pricing (buy together for discount)
    FREE_ITEM,          // Free item with purchase
    TIERED_DISCOUNT,    // Discount increases with quantity
    SPEND_X_GET_Y       // Spend $X get $Y off or free item
}
