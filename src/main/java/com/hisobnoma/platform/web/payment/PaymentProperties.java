package com.hisobnoma.platform.web.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Per-provider payment configuration, bound from {@code hisobnoma.payment.*}.
 *
 * Every provider is <b>disabled by default</b>: with no merchant credentials a
 * provider reports {@link Provider#isConfigured()} = false, and the API returns
 * 503 rather than ever pretending a payment succeeded. Fill these in (ideally
 * from environment variables) once a merchant account + sandbox exist.
 *
 * <pre>
 * hisobnoma:
 *   payment:
 *     payme:
 *       enabled: true
 *       merchant-id: ${PAYME_MERCHANT_ID}
 *       secret-key: ${PAYME_KEY}
 *       checkout-base-url: https://checkout.paycom.uz
 *     click:
 *       enabled: true
 *       merchant-id: ${CLICK_MERCHANT_ID}
 *       service-id: ${CLICK_SERVICE_ID}
 *       secret-key: ${CLICK_SECRET}
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "hisobnoma.payment")
@Getter
@Setter
public class PaymentProperties {

    private Provider payme = new Provider();
    private Provider click = new Provider();
    private Provider uzum = new Provider();

    @Getter
    @Setter
    public static class Provider {
        /** Master switch; even with credentials the provider stays off until true. */
        private boolean enabled = false;
        private String merchantId;
        private String serviceId;   // Click
        private String secretKey;
        private String checkoutBaseUrl;

        /** Configured only when explicitly enabled and a merchant id is present. */
        public boolean isConfigured() {
            return enabled && merchantId != null && !merchantId.isBlank();
        }
    }
}
