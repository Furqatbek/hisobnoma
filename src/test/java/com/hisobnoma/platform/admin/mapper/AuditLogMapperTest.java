package com.hisobnoma.platform.admin.mapper;

import com.hisobnoma.platform.admin.dto.AuditLogDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogMapperTest {

    private AuditLogMapper auditLogMapper;

    @BeforeEach
    void setUp() {
        auditLogMapper = Mappers.getMapper(AuditLogMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        Instant now = Instant.now();
        AuditLog entity = AuditLog.builder()
                .tenantId(1L)
                .userId(10L)
                .username("admin")
                .userFullName("Admin User")
                .action(AuditLog.AuditAction.CREATE)
                .entityType("Product")
                .entityId(100L)
                .entityName("iPhone")
                .module("INVENTORY")
                .description("Product created")
                .oldValues("{\"price\":100}")
                .newValues("{\"price\":120}")
                .changedFields("price")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestUrl("/api/v1/products")
                .requestMethod("POST")
                .actionTimestamp(now)
                .executionTimeMs(150L)
                .success(true)
                .errorMessage(null)
                .correlationId("corr-123")
                .build();
        entity.setId(1L);

        // When
        AuditLogDTO dto = auditLogMapper.toDto(entity);

        // Then
        assertEquals(1L, dto.getId());
        assertEquals(1L, dto.getTenantId());
        assertEquals(10L, dto.getUserId());
        assertEquals("admin", dto.getUsername());
        assertEquals("Admin User", dto.getUserFullName());
        assertEquals(AuditLog.AuditAction.CREATE, dto.getAction());
        assertEquals("Product", dto.getEntityType());
        assertEquals(100L, dto.getEntityId());
        assertEquals("iPhone", dto.getEntityName());
        assertEquals("INVENTORY", dto.getModule());
        assertEquals("Product created", dto.getDescription());
        assertEquals("{\"price\":100}", dto.getOldValues());
        assertEquals("{\"price\":120}", dto.getNewValues());
        assertEquals("price", dto.getChangedFields());
        assertEquals("192.168.1.1", dto.getIpAddress());
        assertEquals("Mozilla/5.0", dto.getUserAgent());
        assertEquals("/api/v1/products", dto.getRequestUrl());
        assertEquals("POST", dto.getRequestMethod());
        assertEquals(now, dto.getActionTimestamp());
        assertEquals(150L, dto.getExecutionTimeMs());
        assertTrue(dto.isSuccess());
        assertNull(dto.getErrorMessage());
        assertEquals("corr-123", dto.getCorrelationId());
    }

    @Test
    void toDto_nullOptionalFields_handledGracefully() {
        // Given
        AuditLog entity = AuditLog.builder()
                .tenantId(1L)
                .action(AuditLog.AuditAction.READ)
                .entityType("Product")
                .entityId(null)
                .entityName(null)
                .module(null)
                .description(null)
                .oldValues(null)
                .newValues(null)
                .changedFields(null)
                .ipAddress(null)
                .userAgent(null)
                .actionTimestamp(Instant.now())
                .success(true)
                .build();

        // When
        AuditLogDTO dto = auditLogMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNull(dto.getEntityId());
        assertNull(dto.getEntityName());
        assertNull(dto.getModule());
        assertNull(dto.getDescription());
        assertNull(dto.getOldValues());
        assertNull(dto.getNewValues());
    }

    @Test
    void toDto_failedFlag_preservedCorrectly() {
        // Given
        AuditLog entity = AuditLog.builder()
                .tenantId(1L)
                .action(AuditLog.AuditAction.LOGIN_FAILED)
                .entityType("User")
                .actionTimestamp(Instant.now())
                .success(false)
                .errorMessage("Bad credentials")
                .build();

        // When
        AuditLogDTO dto = auditLogMapper.toDto(entity);

        // Then
        assertFalse(dto.isSuccess());
        assertEquals("Bad credentials", dto.getErrorMessage());
    }
}
