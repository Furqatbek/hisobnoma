package com.hisobnoma.platform.web.entity;

/**
 * Online payment providers supported for shop card payments.
 * The mobile app sends one of these when starting a payment.
 */
public enum WebPaymentProviderType {
    PAYME,
    CLICK,
    UZUM
}
