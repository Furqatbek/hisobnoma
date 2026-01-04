package com.hisobnoma.platform.pos.entity;

public enum POSPaymentStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    DECLINED("Declined"),
    REFUNDED("Refunded"),
    VOIDED("Voided");

    private final String displayName;

    POSPaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
