package com.hisobnoma.platform.finance.entity;

/**
 * Status enum for AP payments.
 */
public enum APPaymentStatus {
    /**
     * Payment is in draft state.
     */
    DRAFT,

    /**
     * Payment is pending approval.
     */
    PENDING_APPROVAL,

    /**
     * Payment has been approved.
     */
    APPROVED,

    /**
     * Payment has been processed/completed.
     */
    COMPLETED,

    /**
     * Payment has been voided.
     */
    VOIDED,

    /**
     * Payment failed.
     */
    FAILED
}
