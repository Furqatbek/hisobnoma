package com.hisobnoma.platform.web.payment;

import com.hisobnoma.platform.web.entity.WebPaymentProviderType;

import java.math.BigDecimal;

/**
 * Adapter for a single online payment provider. Implementations build the
 * provider's hosted-checkout redirect URL; the actual confirmation arrives
 * asynchronously (webhook) and is applied via {@code WebPaymentService}.
 *
 * Implementations MUST refuse to produce a URL unless fully configured —
 * {@link #isConfigured()} gates this so an un-provisioned environment can
 * never start a "real" payment.
 */
public interface PaymentProvider {

    WebPaymentProviderType type();

    /** True only when merchant credentials are present and the provider is enabled. */
    boolean isConfigured();

    /**
     * Build a hosted-checkout session for the given order.
     *
     * @return the HTTPS redirect URL plus an optional provider reference
     * @throws IllegalStateException if called while {@link #isConfigured()} is false
     */
    Session createSession(Context ctx);

    /** Inputs needed to start a payment. */
    record Context(Long tenantId, String orderNumber, String publicId,
                   BigDecimal amount, String currency, String returnUrl) {
    }

    /** Result of starting a payment. {@code providerReference} may be null for GET-init providers. */
    record Session(String paymentUrl, String providerReference) {
    }
}
