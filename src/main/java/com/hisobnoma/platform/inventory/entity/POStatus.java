package com.hisobnoma.platform.inventory.entity;

/**
 * Purchase Order status enumeration.
 */
public enum POStatus {
    DRAFT,          // Initial state, can be edited
    PENDING,        // Submitted for approval
    APPROVED,       // Approved, ready for receiving
    PARTIAL,        // Partially received
    RECEIVED,       // Fully received
    CANCELLED,      // Cancelled
    CLOSED          // Closed (received and invoiced)
}
