package com.hisobnoma.platform.common.tenant;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TenantAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @InjectMocks
    private TenantAspect aspect;

    /**
     * Note: The actual TenantAspect DOES NOT filter queries or throw
     * IllegalStateException as the plan suggests. Instead, it populates
     * tenantId on a TenantAwareEntity before save operations.
     */
    static class TestEntity extends TenantAwareEntity {
        private String name;
        public TestEntity(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setTenantBeforeSave_tenantContextSet_populatesEntityTenantId() {
        // Given
        TenantContext.setCurrentTenant(5L);
        TestEntity entity = new TestEntity("test");
        assertNull(entity.getTenantId());

        // When
        aspect.setTenantBeforeSave(joinPoint, entity);

        // Then
        assertEquals(5L, entity.getTenantId());
    }

    @Test
    void setTenantBeforeSave_entityAlreadyHasTenantId_doesNotOverwrite() {
        // Given
        TenantContext.setCurrentTenant(5L);
        TestEntity entity = new TestEntity("test");
        entity.setTenantId(99L);

        // When
        aspect.setTenantBeforeSave(joinPoint, entity);

        // Then — existing value preserved
        assertEquals(99L, entity.getTenantId());
    }

    @Test
    void setTenantBeforeSave_noTenantContext_leavesTenantIdNull() {
        // Given — TenantContext is empty (plan says IllegalStateException,
        // but actual aspect silently does nothing)
        TestEntity entity = new TestEntity("test");
        assertNull(entity.getTenantId());

        // When
        aspect.setTenantBeforeSave(joinPoint, entity);

        // Then — no tenant set, no exception
        assertNull(entity.getTenantId());
    }

    @Test
    void setTenantBeforeSave_nonTenantAwareEntity_noop() {
        // Given
        TenantContext.setCurrentTenant(5L);
        Object nonTenantEntity = new Object();

        // When / Then — should not throw
        assertDoesNotThrow(() -> aspect.setTenantBeforeSave(joinPoint, nonTenantEntity));
    }
}
