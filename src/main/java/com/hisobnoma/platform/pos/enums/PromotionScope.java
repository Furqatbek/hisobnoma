package com.hisobnoma.platform.pos.enums;

/**
 * Scope of where the promotion applies.
 */
public enum PromotionScope {
    ORDER,          // Applies to entire order
    LINE_ITEM,      // Applies to specific line items
    SHIPPING,       // Applies to shipping cost
    CATEGORY,       // Applies to items in category
    PRODUCT         // Applies to specific products
}
