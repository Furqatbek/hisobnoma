package com.hisobnoma.platform.pos.enums;

/**
 * Sales channel a promotion applies to. Defaults to POS so that existing
 * promotions never leak into the online shop without an explicit staff action.
 */
public enum PromotionChannel {
    POS,
    WEB,
    ALL
}
