package com.hisobnoma.platform.web.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window limiter: at most {@code maxRequests} per {@code windowMillis}
 * per key. Stale windows are evicted opportunistically on access.
 */
@Component
public class InMemoryCheckoutRateLimiter implements CheckoutRateLimiter {

    static final int DEFAULT_MAX_REQUESTS = 5;
    static final long DEFAULT_WINDOW_MILLIS = 60_000;

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryCheckoutRateLimiter() {
        this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MILLIS, Clock.systemUTC());
    }

    public InMemoryCheckoutRateLimiter(int maxRequests, long windowMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        evictStale(now);
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startMillis >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(existing.startMillis, existing.count + 1);
        });
        return window.count <= maxRequests;
    }

    private void evictStale(long now) {
        if (windows.size() < 10_000) {
            return;
        }
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().startMillis >= windowMillis) {
                it.remove();
            }
        }
    }

    private record Window(long startMillis, int count) {
    }
}
