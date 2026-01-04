package com.hisobnoma.platform.pos.entity;

public enum TransactionStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    VOIDED("Voided"),
    HELD("Held"),
    CANCELLED("Cancelled");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
