package com.hisobnoma.platform.web.payment;

import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Payme (Paycom) — uses the documented GET-initialization method: the checkout
 * URL is {@code {base}/{base64(m=..;ac.order_id=..;a=..)}}. No server call is
 * needed to start; Payme confirms asynchronously via its JSON-RPC Merchant API
 * webhook (wired separately once the merchant account + sandbox exist).
 *
 * Amount is sent in tiyin (UZS × 100), per Payme's API.
 */
@Component
@RequiredArgsConstructor
public class PaymeProvider implements PaymentProvider {

    private static final String DEFAULT_CHECKOUT_BASE = "https://checkout.paycom.uz";

    private final PaymentProperties properties;

    @Override
    public WebPaymentProviderType type() {
        return WebPaymentProviderType.PAYME;
    }

    @Override
    public boolean isConfigured() {
        return properties.getPayme().isConfigured();
    }

    @Override
    public Session createSession(Context ctx) {
        PaymentProperties.Provider cfg = properties.getPayme();
        if (!cfg.isConfigured()) {
            throw new IllegalStateException("Payme is not configured");
        }
        long tiyin = ctx.amount().setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).longValueExact();

        StringBuilder params = new StringBuilder()
                .append("m=").append(cfg.getMerchantId())
                .append(";ac.order_id=").append(ctx.orderNumber())
                .append(";a=").append(tiyin);
        if (ctx.returnUrl() != null) {
            params.append(";c=").append(ctx.returnUrl());
        }

        String encoded = Base64.getEncoder()
                .encodeToString(params.toString().getBytes(StandardCharsets.UTF_8));
        String base = cfg.getCheckoutBaseUrl() != null ? cfg.getCheckoutBaseUrl() : DEFAULT_CHECKOUT_BASE;
        return new Session(base + "/" + encoded, null);
    }
}
