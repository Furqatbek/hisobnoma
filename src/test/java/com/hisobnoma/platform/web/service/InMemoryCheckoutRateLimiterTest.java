package com.hisobnoma.platform.web.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCheckoutRateLimiterTest {

    /**
     * Clock whose time can be advanced manually.
     */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void allowsUpToMaxRequestsWithinWindow() {
        var limiter = new InMemoryCheckoutRateLimiter(3, 60_000, new MutableClock());

        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
        assertFalse(limiter.tryAcquire("key"));
        assertFalse(limiter.tryAcquire("key"));
    }

    @Test
    void windowResetsAfterExpiry() {
        var clock = new MutableClock();
        var limiter = new InMemoryCheckoutRateLimiter(2, 60_000, clock);

        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
        assertFalse(limiter.tryAcquire("key"));

        clock.advanceMillis(60_001);

        assertTrue(limiter.tryAcquire("key"));
        assertTrue(limiter.tryAcquire("key"));
        assertFalse(limiter.tryAcquire("key"));
    }

    @Test
    void keysAreIndependent() {
        var limiter = new InMemoryCheckoutRateLimiter(1, 60_000, new MutableClock());

        assertTrue(limiter.tryAcquire("ip1|phone1"));
        assertFalse(limiter.tryAcquire("ip1|phone1"));
        assertTrue(limiter.tryAcquire("ip2|phone2"));
    }
}
