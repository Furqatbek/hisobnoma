package com.hisobnoma.platform.finance.entity;

/**
 * Enum representing the source/origin of a journal entry.
 */
public enum JournalSource {
    /**
     * Manual journal entry created by user
     */
    MANUAL,

    /**
     * Entry from POS sales transaction
     */
    POS_SALE,

    /**
     * Entry from stock receiving
     */
    RECEIVING,

    /**
     * Entry from inventory adjustment
     */
    INVENTORY_ADJUSTMENT,

    /**
     * Entry from accounts payable
     */
    ACCOUNTS_PAYABLE,

    /**
     * Entry from accounts receivable
     */
    ACCOUNTS_RECEIVABLE,

    /**
     * Entry from payment processing
     */
    PAYMENT,

    /**
     * Entry from recurring journal template
     */
    RECURRING,

    /**
     * Entry from year-end closing
     */
    YEAR_END_CLOSING,

    /**
     * Entry from HR salary payment
     */
    SALARY_PAYMENT,

    /**
     * System generated entry
     */
    SYSTEM
}
