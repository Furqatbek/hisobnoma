package com.hisobnoma.platform.pos.entity;

public enum ShiftStatus {
    OPEN("Open"),
    CLOSED("Closed"),
    RECONCILED("Reconciled");

    private final String displayName;

    ShiftStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
