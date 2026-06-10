package com.hisobnoma.platform.web.security;

import com.hisobnoma.platform.web.entity.WebCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WebCustomerTokenServiceTest {

    private static final String SECRET = "test-256-bit-secret-key-for-web-customer-token-tests";

    private WebCustomerTokenService service;

    private WebCustomerTokenService buildService(String secret, long expirationMillis) {
        WebCustomerTokenService s = new WebCustomerTokenService();
        ReflectionTestUtils.setField(s, "jwtSecret", secret);
        ReflectionTestUtils.setField(s, "expirationMillis", expirationMillis);
        ReflectionTestUtils.invokeMethod(s, "init");
        return s;
    }

    private WebCustomer customer() {
        WebCustomer c = WebCustomer.builder()
                .phone("998901234567")
                .tenantId(7L)
                .build();
        c.setId(42L);
        return c;
    }

    @BeforeEach
    void setUp() {
        service = buildService(SECRET, 60_000);
    }

    @Test
    void generatedTokenParsesBackToPrincipal() {
        String token = service.generateToken(customer());

        Optional<WebCustomerTokenService.WebCustomerPrincipal> principal = service.parse(token);

        assertTrue(principal.isPresent());
        assertEquals(42L, principal.get().webCustomerId());
        assertEquals(7L, principal.get().tenantId());
        assertEquals("998901234567", principal.get().phone());
    }

    @Test
    void expiredTokenIsRejected() {
        WebCustomerTokenService shortLived = buildService(SECRET, -1000);
        String token = shortLived.generateToken(customer());

        assertTrue(shortLived.parse(token).isEmpty());
    }

    @Test
    void garbageTokenIsRejected() {
        assertTrue(service.parse("not.a.token").isEmpty());
        assertTrue(service.parse("").isEmpty());
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        WebCustomerTokenService other = buildService("another-secret-key-entirely-different-0001", 60_000);
        String foreign = other.generateToken(customer());

        assertTrue(service.parse(foreign).isEmpty());
    }
}
