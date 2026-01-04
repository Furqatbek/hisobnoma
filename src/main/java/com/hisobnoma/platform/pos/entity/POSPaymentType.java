package com.hisobnoma.platform.pos.entity;

public enum POSPaymentType {
    CASH("Cash"),
    CARD("Card"),
    CREDIT("Credit/Account"),
    GIFT_CARD("Gift Card"),
    MOBILE_PAYMENT("Mobile Payment"),
    CHECK("Check"),
    OTHER("Other");

    private final String displayName;

    POSPaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
