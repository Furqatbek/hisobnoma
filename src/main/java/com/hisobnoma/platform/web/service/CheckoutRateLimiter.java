package com.hisobnoma.platform.web.service;

/**
 * Throttles anonymous checkout/lookup calls. The in-memory implementation
 * is sufficient for a single-instance deployment; a Redis-backed
 * implementation can replace it without touching callers.
 */
public interface CheckoutRateLimiter {

    /**
     * @param key client identity (e.g. "ip|phone")
     * @return true when the call is allowed, false when the limit is exhausted
     */
    boolean tryAcquire(String key);
}
