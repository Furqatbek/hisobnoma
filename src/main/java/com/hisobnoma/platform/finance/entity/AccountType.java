package com.hisobnoma.platform.finance.entity;

/**
 * Enum representing the type of account in the chart of accounts.
 */
public enum AccountType {
    /**
     * Assets - Resources owned by the company
     */
    ASSET,

    /**
     * Liabilities - Obligations owed to others
     */
    LIABILITY,

    /**
     * Equity - Owner's interest in the business
     */
    EQUITY,

    /**
     * Revenue - Income from business operations
     */
    REVENUE,

    /**
     * Expense - Costs incurred in operations
     */
    EXPENSE
}
