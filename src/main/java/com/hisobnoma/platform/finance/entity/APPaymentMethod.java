package com.hisobnoma.platform.finance.entity;

/**
 * Payment method enum for AP payments.
 */
public enum APPaymentMethod {
    /**
     * Cash payment.
     */
    CASH,

    /**
     * Check payment.
     */
    CHECK,

    /**
     * Bank transfer / Wire transfer.
     */
    BANK_TRANSFER,

    /**
     * Credit card payment.
     */
    CREDIT_CARD,

    /**
     * ACH / Direct debit.
     */
    ACH,

    /**
     * Online payment (PayPal, etc.).
     */
    ONLINE,

    /**
     * Other payment method.
     */
    OTHER
}
