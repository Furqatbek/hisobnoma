package com.hisobnoma.platform.web.payment;

import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Click — redirects to the hosted pay page
 * {@code https://my.click.uz/services/pay} with the merchant/service ids and
 * the order number as {@code transaction_param}. Click confirms asynchronously
 * via its prepare/complete callbacks (wired separately once credentials exist).
 *
 * Amount is sent in so'm (UZS), per Click's pay-link spec.
 */
@Component
@RequiredArgsConstructor
public class ClickProvider implements PaymentProvider {

    private static final String DEFAULT_CHECKOUT_BASE = "https://my.click.uz/services/pay";

    private final PaymentProperties properties;

    @Override
    public WebPaymentProviderType type() {
        return WebPaymentProviderType.CLICK;
    }

    @Override
    public boolean isConfigured() {
        PaymentProperties.Provider cfg = properties.getClick();
        return cfg.isConfigured() && cfg.getServiceId() != null && !cfg.getServiceId().isBlank();
    }

    @Override
    public Session createSession(Context ctx) {
        PaymentProperties.Provider cfg = properties.getClick();
        if (!isConfigured()) {
            throw new IllegalStateException("Click is not configured");
        }
        BigDecimal amount = ctx.amount().setScale(2, RoundingMode.HALF_UP);
        String base = cfg.getCheckoutBaseUrl() != null ? cfg.getCheckoutBaseUrl() : DEFAULT_CHECKOUT_BASE;

        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(base)
                .queryParam("service_id", cfg.getServiceId())
                .queryParam("merchant_id", cfg.getMerchantId())
                .queryParam("amount", amount.toPlainString())
                .queryParam("transaction_param", ctx.orderNumber());
        if (ctx.returnUrl() != null) {
            url.queryParam("return_url", ctx.returnUrl());
        }
        return new Session(url.build().toUriString(), null);
    }
}
