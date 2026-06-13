package com.hisobnoma.platform.web.payment;

import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Uzum Bank checkout. The redirect is built from configured merchant details.
 *
 * NOTE: Uzum's exact hosted-checkout link format and callback contract must be
 * confirmed against their merchant docs/sandbox before go-live — this builds a
 * reasonable parameterized URL, gated on configuration, so nothing activates
 * until {@code hisobnoma.payment.uzum.enabled=true} with a merchant id.
 */
@Component
@RequiredArgsConstructor
public class UzumProvider implements PaymentProvider {

    private static final String DEFAULT_CHECKOUT_BASE = "https://checkout.uzumbank.uz/pay";

    private final PaymentProperties properties;

    @Override
    public WebPaymentProviderType type() {
        return WebPaymentProviderType.UZUM;
    }

    @Override
    public boolean isConfigured() {
        return properties.getUzum().isConfigured();
    }

    @Override
    public Session createSession(Context ctx) {
        PaymentProperties.Provider cfg = properties.getUzum();
        if (!cfg.isConfigured()) {
            throw new IllegalStateException("Uzum is not configured");
        }
        BigDecimal amount = ctx.amount().setScale(2, RoundingMode.HALF_UP);
        String base = cfg.getCheckoutBaseUrl() != null ? cfg.getCheckoutBaseUrl() : DEFAULT_CHECKOUT_BASE;

        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(base)
                .queryParam("merchantId", cfg.getMerchantId())
                .queryParam("orderId", ctx.orderNumber())
                .queryParam("amount", amount.toPlainString());
        if (ctx.returnUrl() != null) {
            url.queryParam("returnUrl", ctx.returnUrl());
        }
        return new Session(url.build().toUriString(), null);
    }
}
