package com.hisobnoma.platform.web.payment;

import com.hisobnoma.platform.web.payment.PaymentProvider.Context;
import com.hisobnoma.platform.web.payment.PaymentProvider.Session;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderTest {

    private Context ctx() {
        return new Context(1L, "WO-000042", "pay_abc",
                new BigDecimal("33000"), "UZS", "https://app.example/return");
    }

    // ---- not configured by default ----

    @Test
    void providers_areNotConfiguredByDefault() {
        PaymentProperties props = new PaymentProperties();
        assertFalse(new PaymeProvider(props).isConfigured());
        assertFalse(new ClickProvider(props).isConfigured());
        assertFalse(new UzumProvider(props).isConfigured());
    }

    @Test
    void payme_throwsWhenUnconfigured() {
        assertThrows(IllegalStateException.class,
                () -> new PaymeProvider(new PaymentProperties()).createSession(ctx()));
    }

    // ---- Payme GET-init URL ----

    @Test
    void payme_buildsBase64CheckoutUrlInTiyin() {
        PaymentProperties props = new PaymentProperties();
        props.getPayme().setEnabled(true);
        props.getPayme().setMerchantId("MID123");

        Session session = new PaymeProvider(props).createSession(ctx());

        assertTrue(session.paymentUrl().startsWith("https://checkout.paycom.uz/"));
        String encoded = session.paymentUrl().substring("https://checkout.paycom.uz/".length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("m=MID123"));
        assertTrue(decoded.contains("ac.order_id=WO-000042"));
        assertTrue(decoded.contains("a=3300000")); // 33000 UZS -> tiyin
        assertTrue(decoded.contains("c=https://app.example/return"));
    }

    // ---- Click pay URL ----

    @Test
    void click_requiresServiceIdToBeConfigured() {
        PaymentProperties props = new PaymentProperties();
        props.getClick().setEnabled(true);
        props.getClick().setMerchantId("M1");
        // serviceId missing
        assertFalse(new ClickProvider(props).isConfigured());
    }

    @Test
    void click_buildsPayUrlWithMerchantAndOrder() {
        PaymentProperties props = new PaymentProperties();
        props.getClick().setEnabled(true);
        props.getClick().setMerchantId("M1");
        props.getClick().setServiceId("S1");

        Session session = new ClickProvider(props).createSession(ctx());

        assertTrue(session.paymentUrl().startsWith("https://my.click.uz/services/pay"));
        assertTrue(session.paymentUrl().contains("service_id=S1"));
        assertTrue(session.paymentUrl().contains("merchant_id=M1"));
        assertTrue(session.paymentUrl().contains("transaction_param=WO-000042"));
        assertTrue(session.paymentUrl().contains("amount=33000"));
    }
}
