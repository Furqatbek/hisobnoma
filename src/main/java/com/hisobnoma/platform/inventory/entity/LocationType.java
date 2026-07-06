package com.hisobnoma.platform.inventory.entity;

/**
 * Enum representing the type of inventory location.
 */
public enum LocationType {
    /**
     * Main warehouse storage location
     */
    WAREHOUSE,

    /**
     * Retail store location
     */
    STORE,

    /**
     * Virtual location for transit, adjustments, etc.
     */
    VIRTUAL,

    /**
     * Zone within a warehouse
     */
    ZONE,

    /**
     * Storage bin within a zone
     */
    BIN,

    /**
     * A vehicle (van/truck) used for mobile/van sales — stock is loaded onto it,
     * sold in the field, and reconciled back.
     */
    VEHICLE
}
