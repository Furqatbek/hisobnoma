package com.hisobnoma.platform.finance.entity;

/**
 * Status values for Credit Notes.
 */
public enum CreditNoteStatus {
    DRAFT,              // Credit note created but not applied
    APPROVED,           // Approved and ready to apply
    APPLIED,            // Applied to invoice(s)
    PARTIAL,            // Partially applied
    CANCELLED           // Cancelled/voided
}
