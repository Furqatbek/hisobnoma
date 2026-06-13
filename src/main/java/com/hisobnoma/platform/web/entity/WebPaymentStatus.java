package com.hisobnoma.platform.web.entity;

/**
 * Payment state of an online order, surfaced to the mobile app as a status
 * pill. Distinct from {@link WebOrderStatus} (the fulfilment lifecycle).
 *
 * <ul>
 *   <li>{@code NONE}      — no online payment applies (e.g. cash on delivery)</li>
 *   <li>{@code PENDING}   — a card payment was started, awaiting provider result</li>
 *   <li>{@code PAID}      — provider confirmed the payment</li>
 *   <li>{@code FAILED}    — provider declined / the payment errored</li>
 *   <li>{@code CANCELLED} — the customer abandoned the payment</li>
 *   <li>{@code REFUNDED}  — a previously paid order was refunded/cancelled</li>
 * </ul>
 */
public enum WebPaymentStatus {
    NONE,
    PENDING,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED
}
