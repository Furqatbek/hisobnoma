package com.hisobnoma.platform.web.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCheckoutRateLimiterTest {

    @Mock private StringRedisTemplate redis;

    @SuppressWarnings("unchecked")
    private void stubCount(Long count) {
        when(redis.execute(any(RedisScript.class), any(List.class), anyString())).thenReturn(count);
    }

    @Test
    void allowsWhileUnderLimit() {
        RedisCheckoutRateLimiter limiter = new RedisCheckoutRateLimiter(redis);
        stubCount(1L);
        assertTrue(limiter.tryAcquire("ip|phone"));
        stubCount(5L);
        assertTrue(limiter.tryAcquire("ip|phone"), "5th call within the window is allowed");
    }

    @Test
    void deniesOnceOverLimit() {
        RedisCheckoutRateLimiter limiter = new RedisCheckoutRateLimiter(redis);
        stubCount(6L);
        assertFalse(limiter.tryAcquire("ip|phone"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsOpenWhenRedisUnavailable() {
        RedisCheckoutRateLimiter limiter = new RedisCheckoutRateLimiter(redis);
        when(redis.execute(any(RedisScript.class), any(List.class), anyString()))
                .thenThrow(new RuntimeException("connection refused"));
        assertTrue(limiter.tryAcquire("ip|phone"), "a Redis outage must not block checkout");
    }
}
