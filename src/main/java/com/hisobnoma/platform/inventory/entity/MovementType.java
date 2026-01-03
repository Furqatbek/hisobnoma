package com.hisobnoma.platform.inventory.entity;

/**
 * Enum representing the type of stock movement.
 */
public enum MovementType {
    /**
     * Stock received from purchase or return
     */
    STOCK_IN,

    /**
     * Stock issued for sale or use
     */
    STOCK_OUT,

    /**
     * Stock transferred between locations
     */
    TRANSFER,

    /**
     * Stock adjustment (positive or negative)
     */
    ADJUSTMENT,

    /**
     * Initial stock entry
     */
    INITIAL,

    /**
     * Stock return from customer
     */
    RETURN,

    /**
     * Stock reserved for an order
     */
    RESERVATION,

    /**
     * Stock unreserved (cancelled order)
     */
    UNRESERVATION,

    /**
     * Stock write-off (damaged, expired, etc.)
     */
    WRITE_OFF,

    /**
     * Stock from production/manufacturing
     */
    PRODUCTION,

    /**
     * Stock used in production/manufacturing
     */
    CONSUMPTION
}
