package com.hisobnoma.platform.pos.enums;

/**
 * Types of price lists.
 */
public enum PriceListType {
    STANDARD,       // Default pricing for all customers
    WHOLESALE,      // Wholesale/bulk pricing
    VIP,            // VIP/loyalty customer pricing
    SEASONAL,       // Seasonal/promotional pricing
    EMPLOYEE,       // Employee discount pricing
    CUSTOM          // Custom price list for specific customers
}
