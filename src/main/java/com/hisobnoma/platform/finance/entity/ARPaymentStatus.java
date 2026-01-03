package com.hisobnoma.platform.finance.entity;

/**
 * Status values for AR (Accounts Receivable) payments.
 */
public enum ARPaymentStatus {
    PENDING,            // Payment recorded but not yet processed
    COMPLETED,          // Payment successfully processed
    FAILED,             // Payment processing failed
    REFUNDED,           // Payment was refunded
    CANCELLED,          // Payment was cancelled
    BOUNCED             // Check/payment bounced
}
