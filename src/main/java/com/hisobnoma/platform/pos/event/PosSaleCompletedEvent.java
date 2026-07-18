package com.hisobnoma.platform.pos.event;

/**
 * Published when a POS transaction is completed. A GL posting listener handles it AFTER the sale's
 * transaction commits, so the general-ledger entry is only ever created for a sale that actually
 * committed — closing the phantom-revenue window that existed when GL was posted inline in a
 * REQUIRES_NEW transaction during completion.
 */
public record PosSaleCompletedEvent(Long transactionId) {
}
