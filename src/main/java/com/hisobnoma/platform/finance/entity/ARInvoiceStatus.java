package com.hisobnoma.platform.finance.entity;

/**
 * Status values for AR (Accounts Receivable) invoices.
 */
public enum ARInvoiceStatus {
    DRAFT,              // Invoice created but not finalized
    PENDING,            // Invoice pending customer review
    SENT,               // Invoice sent to customer
    PARTIAL,            // Partially paid
    PAID,               // Fully paid
    OVERDUE,            // Past due date and unpaid
    CANCELLED,          // Cancelled/voided
    DISPUTED,           // Customer disputes the invoice
    WRITTEN_OFF         // Written off as bad debt
}
