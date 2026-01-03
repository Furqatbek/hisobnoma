package com.hisobnoma.platform.inventory.entity;

/**
 * Inventory Count type enumeration.
 */
public enum CountType {
    FULL,           // Full inventory count at location
    PARTIAL,        // Partial count (selected products)
    CYCLE,          // Cycle count (ABC-based)
    SPOT,           // Spot check
    ANNUAL          // Annual count
}
