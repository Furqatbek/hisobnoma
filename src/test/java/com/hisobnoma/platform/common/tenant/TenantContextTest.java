package com.hisobnoma.platform.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
}
