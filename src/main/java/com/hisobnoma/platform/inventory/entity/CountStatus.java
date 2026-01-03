package com.hisobnoma.platform.inventory.entity;

/**
 * Inventory Count status enumeration.
 */
public enum CountStatus {
    DRAFT,          // Initial state, selecting products
    IN_PROGRESS,    // Count is in progress
    COMPLETED,      // Count is complete, pending approval
    APPROVED,       // Approved, adjustments posted
    CANCELLED       // Cancelled
}
