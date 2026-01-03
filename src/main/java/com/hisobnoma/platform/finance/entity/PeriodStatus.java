package com.hisobnoma.platform.finance.entity;

/**
 * Enum representing the status of a fiscal period.
 */
public enum PeriodStatus {
    /**
     * Period is open for posting
     */
    OPEN,

    /**
     * Period is closed - no more postings allowed
     */
    CLOSED,

    /**
     * Period is locked - permanently sealed, year-end completed
     */
    LOCKED
}
