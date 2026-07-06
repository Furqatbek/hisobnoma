package com.hisobnoma.platform.distribution.entity;

/**
 * How a distribution order is settled.
 */
public enum DistributionPaymentMethod {
    /** Paid in full on delivery. */
    CASH,
    /** Billed to the customer's AR account on terms. */
    CREDIT,
    /** Part cash on delivery, remainder on credit. */
    MIXED
}
