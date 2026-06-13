package com.hisobnoma.platform.web.entity;

/**
 * How the customer intends to pay for an online order.
 * Sent by the mobile app at checkout and echoed on the order DTO.
 */
public enum WebPaymentMethod {
    /** Cash on delivery — no online payment expected. */
    CASH,
    /** Online card payment via a provider (Payme/Click/Uzum). */
    CARD
}
