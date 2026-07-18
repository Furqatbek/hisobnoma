package com.hisobnoma.platform.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Distributed fixed-window rate limiter backed by Redis, so per-IP/per-phone limits hold across all
 * app instances (the in-memory limiter counts per-JVM, giving N× the limit with N pods). Active only
 * when {@code hisobnoma.ratelimit.redis-enabled=true} (set in the prod profile); otherwise the
 * in-memory limiter is used, so dev/test need no Redis.
 *
 * <p>Counting is atomic via a small Lua script (INCR + PEXPIRE-on-first-hit), matching the in-memory
 * limiter's semantics: at most {@code maxRequests} per {@code windowMillis} per key. If Redis is
 * unreachable the limiter fails OPEN (allows the call) and logs — a transient Redis blip must not
 * take down checkout; the DB-level caps (e.g. 5 OTP/phone/day) still bound the expensive paths.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(prefix = "hisobnoma.ratelimit", name = "redis-enabled", havingValue = "true")
public class RedisCheckoutRateLimiter implements CheckoutRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MILLIS = 60_000;
    private static final String KEY_PREFIX = "ratelimit:";

    /** INCR the counter; on the first hit set the window TTL. Returns the current count. */
    private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1])\n"
            + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n"
            + "return c", Long.class);

    private final StringRedisTemplate redis;

    public RedisCheckoutRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        log.info("Rate limiting is backed by Redis (distributed): {}/{}ms per key",
                MAX_REQUESTS, WINDOW_MILLIS);
    }

    @Override
    public boolean tryAcquire(String key) {
        try {
            Long count = redis.execute(INCR_WITH_TTL, List.of(KEY_PREFIX + key),
                    String.valueOf(WINDOW_MILLIS));
            return count == null || count <= MAX_REQUESTS;
        } catch (Exception e) {
            // Fail open: never block legitimate traffic on a Redis outage.
            log.warn("Redis rate-limit check failed for key '{}', allowing: {}", key, e.getMessage());
            return true;
        }
    }
}
