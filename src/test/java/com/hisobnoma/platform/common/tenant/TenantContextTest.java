package com.hisobnoma.platform.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetCurrentTenant() {
        // Given
        Long tenantId = 123L;

        // When
        TenantContext.setCurrentTenant(tenantId);

        // Then
        assertEquals(tenantId, TenantContext.getCurrentTenant());
    }

    @Test
    void shouldReturnNullWhenNoTenantSet() {
        // When/Then
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldClearTenantContext() {
        // Given
        TenantContext.setCurrentTenant(123L);

        // When
        TenantContext.clear();

        // Then
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldReturnHasTenantCorrectly() {
        // Given - no tenant
        assertFalse(TenantContext.hasTenant());

        // When
        TenantContext.setCurrentTenant(123L);

        // Then
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    void shouldIsolateTenantBetweenThreads() throws InterruptedException {
        // Given
        TenantContext.setCurrentTenant(1L);

        Thread otherThread = new Thread(() -> {
            TenantContext.setCurrentTenant(2L);
            assertEquals(2L, TenantContext.getCurrentTenant());
        });

        // When
        otherThread.start();
        otherThread.join();

        // Then - main thread still has tenant 1
        assertEquals(1L, TenantContext.getCurrentTenant());
    }

    @Test
    void threadIsolation_eachThreadSeesOwnValue_withCountDownLatchSync() throws InterruptedException {
        // Given — two threads independently set and read their own values
        CountDownLatch setLatch = new CountDownLatch(2);
        CountDownLatch readLatch = new CountDownLatch(1);
        AtomicReference<Long> threadAResult = new AtomicReference<>();
        AtomicReference<Long> threadBResult = new AtomicReference<>();

        Thread threadA = new Thread(() -> {
            try {
                TenantContext.setCurrentTenant(1L);
                setLatch.countDown();
                readLatch.await();
                threadAResult.set(TenantContext.getCurrentTenant());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                TenantContext.clear();
            }
        });

        Thread threadB = new Thread(() -> {
            try {
                TenantContext.setCurrentTenant(2L);
                setLatch.countDown();
                readLatch.await();
                threadBResult.set(TenantContext.getCurrentTenant());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                TenantContext.clear();
            }
        });

        // When
        threadA.start();
        threadB.start();
        assertTrue(setLatch.await(5, TimeUnit.SECONDS));
        readLatch.countDown();
        threadA.join(5000);
        threadB.join(5000);

        // Then
        assertEquals(1L, threadAResult.get());
        assertEquals(2L, threadBResult.get());
    }

    @Test
    void clearInFinallyBlock_noLeakBetweenRequests() {
        // Given — first pseudo-request sets and clears in finally
        try {
            TenantContext.setCurrentTenant(10L);
            assertEquals(10L, TenantContext.getCurrentTenant());
        } finally {
            TenantContext.clear();
        }

        // When — second pseudo-request reads before setting anything
        Long secondRequestTenant = TenantContext.getCurrentTenant();

        // Then — should see null, not 10L
        assertNull(secondRequestTenant);
    }
}
