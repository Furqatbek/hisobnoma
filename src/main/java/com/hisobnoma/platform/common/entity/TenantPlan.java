package com.hisobnoma.platform.common.entity;

import java.math.BigDecimal;

/**
 * Subscription tiers (see docs/MULTI_TENANCY.md §9). Limits are applied to the
 * tenant row on every plan change; prices are informational until recurring
 * billing lands — plan changes are self-service and take effect immediately.
 */
public enum TenantPlan {

    FREE("Бепул", 2, 1, BigDecimal.ZERO),
    STARTER("Стартер", 5, 2, new BigDecimal("99000")),
    BUSINESS("Бизнес", 25, 5, new BigDecimal("299000")),
    ENTERPRISE("Корпоратив", 100, 20, new BigDecimal("999000"));

    private final String displayName;
    private final int maxUsers;
    private final int maxLocations;
    private final BigDecimal monthlyPrice;

    TenantPlan(String displayName, int maxUsers, int maxLocations, BigDecimal monthlyPrice) {
        this.displayName = displayName;
        this.maxUsers = maxUsers;
        this.maxLocations = maxLocations;
        this.monthlyPrice = monthlyPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getMaxLocations() {
        return maxLocations;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }
}
