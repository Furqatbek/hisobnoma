package com.hisobnoma.platform.finance.entity;

/**
 * Status enum for AP (Accounts Payable) invoices.
 */
public enum APInvoiceStatus {
    /**
     * Invoice is in draft state, not yet submitted for approval.
     */
    DRAFT,

    /**
     * Invoice is pending approval.
     */
    PENDING_APPROVAL,

    /**
     * Invoice has been approved and is ready for payment.
     */
    APPROVED,

    /**
     * Invoice has been partially paid.
     */
    PARTIAL,

    /**
     * Invoice has been fully paid.
     */
    PAID,

    /**
     * Invoice has been cancelled/voided.
     */
    CANCELLED,

    /**
     * Invoice is on hold (disputed or waiting for additional info).
     */
    ON_HOLD
}
