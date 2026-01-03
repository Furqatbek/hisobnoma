package com.hisobnoma.platform.inventory.entity;

/**
 * Enum representing the source/reference type for a stock movement.
 */
public enum MovementReferenceType {
    /**
     * From a purchase order
     */
    PURCHASE_ORDER,

    /**
     * From a receiving order
     */
    RECEIVING_ORDER,

    /**
     * From a POS transaction
     */
    POS_TRANSACTION,

    /**
     * From a web order
     */
    WEB_ORDER,

    /**
     * From a manual adjustment
     */
    MANUAL_ADJUSTMENT,

    /**
     * From an inventory count
     */
    INVENTORY_COUNT,

    /**
     * From a stock transfer
     */
    TRANSFER_ORDER,

    /**
     * From a sales return
     */
    SALES_RETURN,

    /**
     * From a purchase return
     */
    PURCHASE_RETURN,

    /**
     * System generated
     */
    SYSTEM,

    /**
     * From production order
     */
    PRODUCTION_ORDER
}
