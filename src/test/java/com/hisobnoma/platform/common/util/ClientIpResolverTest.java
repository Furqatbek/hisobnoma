package com.hisobnoma.platform.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void trustingProxy_prefersXRealIp() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Real-IP", "203.0.113.7");
        // Client tried to forge an origin at the front of XFF — X-Real-IP wins.
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.7, 10.0.0.5");

        assertEquals("203.0.113.7", resolver.resolve(request));
    }

    @Test
    void trustingProxy_noRealIp_usesRightmostForwardedEntryNotSpoofableLeftmost() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        // Attacker prepends a fake IP; nginx appends the real peer at the end.
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.7");

        assertEquals("203.0.113.7", resolver.resolve(request));
    }

    @Test
    void trustingProxy_noHeader_fallsBackToRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void notTrustingProxy_ignoresSpoofableHeader() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertEquals("198.51.100.9", resolver.resolve(request));
    }
}
