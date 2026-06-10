package com.hisobnoma.platform.web.entity;

/**
 * Fixed v1 customer segments for SMS campaigns. Each targets verified web
 * customer accounts (joined to their orders by normalized phone) and always
 * excludes customers who opted out of SMS.
 */
public enum WebSegmentType {
    /** Every web customer. */
    ALL_CUSTOMERS(false),
    /** Ordered at least once within the last N days. */
    ORDERED_LAST_N_DAYS(true),
    /** Ordered before, but nothing in the last N days (sleeping customers). */
    NO_ORDER_IN_N_DAYS(true),
    /** Lifetime completed-order spend ≥ the given amount. */
    MIN_TOTAL_SPENT(true),
    /** Has an account but has never placed an order. */
    NEVER_ORDERED(false);

    private final boolean requiresParam;

    WebSegmentType(boolean requiresParam) {
        this.requiresParam = requiresParam;
    }

    public boolean requiresParam() {
        return requiresParam;
    }
}
