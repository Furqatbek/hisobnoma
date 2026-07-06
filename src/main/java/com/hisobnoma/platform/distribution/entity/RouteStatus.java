package com.hisobnoma.platform.distribution.entity;

/**
 * Lifecycle of a distribution route plan.
 */
public enum RouteStatus {
    /** Being drafted; not yet in use. */
    DRAFT,
    /** In active use for scheduling visits. */
    ACTIVE,
    /** Retired but kept for history. */
    ARCHIVED
}
