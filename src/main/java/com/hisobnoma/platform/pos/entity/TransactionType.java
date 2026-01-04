package com.hisobnoma.platform.pos.entity;

public enum TransactionType {
    SALE("Sale"),
    RETURN("Return"),
    EXCHANGE("Exchange");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
