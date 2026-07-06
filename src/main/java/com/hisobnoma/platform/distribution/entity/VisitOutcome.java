package com.hisobnoma.platform.distribution.entity;

/**
 * Result of a visit, set at check-out. Starts {@code PENDING} on check-in.
 */
public enum VisitOutcome {
    PENDING,
    ORDER_PLACED,
    NO_ORDER,
    PAYMENT_COLLECTED,
    RESCHEDULED,
    CLOSED
}
