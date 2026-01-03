package com.hisobnoma.platform.finance.entity;

/**
 * Enum representing the status of a journal entry.
 */
public enum JournalStatus {
    /**
     * Draft - Entry created but not yet posted
     */
    DRAFT,

    /**
     * Posted - Entry has been posted to the general ledger
     */
    POSTED,

    /**
     * Reversed - Entry has been reversed
     */
    REVERSED
}
