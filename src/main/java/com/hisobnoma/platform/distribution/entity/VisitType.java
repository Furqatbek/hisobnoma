package com.hisobnoma.platform.distribution.entity;

/**
 * Why a visit happened.
 */
public enum VisitType {
    /** A stop on the agent's planned route. */
    PLANNED,
    /** An unplanned visit outside the route. */
    AD_HOC,
    /** A follow-up to an earlier visit. */
    RETURN_VISIT
}
