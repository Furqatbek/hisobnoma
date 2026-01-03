package com.hisobnoma.platform.inventory.entity;

/**
 * Receiving Order status enumeration.
 */
public enum ReceivingStatus {
    DRAFT,          // Initial state, can be edited
    PENDING,        // Submitted, awaiting receiving
    IN_PROGRESS,    // Currently being received
    COMPLETED,      // Fully received
    CANCELLED       // Cancelled
}
