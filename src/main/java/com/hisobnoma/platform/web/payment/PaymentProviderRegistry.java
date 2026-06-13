package com.hisobnoma.platform.web.payment;

import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a {@link PaymentProvider} by type. All adapters are injected by
 * Spring; lookups are O(1).
 */
@Component
public class PaymentProviderRegistry {

    private final Map<WebPaymentProviderType, PaymentProvider> providers =
            new EnumMap<>(WebPaymentProviderType.class);

    public PaymentProviderRegistry(List<PaymentProvider> all) {
        for (PaymentProvider p : all) {
            providers.put(p.type(), p);
        }
    }

    /** @return the adapter for the given type, or null if none is registered. */
    public PaymentProvider get(WebPaymentProviderType type) {
        return providers.get(type);
    }
}
