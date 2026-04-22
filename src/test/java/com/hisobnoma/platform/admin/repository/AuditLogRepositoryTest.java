package com.hisobnoma.platform.admin.repository;

import com.hisobnoma.platform.admin.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private Instant baseTime;

    @BeforeEach
    void setUp() {
        baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private AuditLog createLog(Long tenantId, Long userId, String username,
                                AuditLog.AuditAction action, String entityType,
                                String module, Instant timestamp, boolean success) {
        AuditLog log = AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(1L)
                .module(module)
                .description("test")
                .actionTimestamp(timestamp)
                .success(success)
                .build();
        return entityManager.persistAndFlush(log);
    }

    // ---- findByTenantIdAndDateRange ----

    @Test
    void findByTenantIdAndDateRange_withinRange_returnsMatchingLogs() {
        // Given
        Instant rangeStart = baseTime;
        Instant rangeEnd = baseTime.plus(3, ChronoUnit.HOURS);
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.CREATE, "Product", "INVENTORY",
                baseTime.plus(1, ChronoUnit.HOURS), true);
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.UPDATE, "Product", "INVENTORY",
                baseTime.plus(2, ChronoUnit.HOURS), true);

        // When
        Page<AuditLog> result = auditLogRepository.findByTenantIdAndDateRange(
                TENANT_ID, rangeStart, rangeEnd, PageRequest.of(0, 20));

        // Then
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndDateRange_outsideRange_excluded() {
        // Given
        Instant rangeStart = baseTime;
        Instant rangeEnd = baseTime.plus(3, ChronoUnit.HOURS);
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.CREATE, "Product", "INVENTORY",
                baseTime.minus(1, ChronoUnit.HOURS), true);
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.UPDATE, "Product", "INVENTORY",
                baseTime.plus(5, ChronoUnit.HOURS), true);

        // When
        Page<AuditLog> result = auditLogRepository.findByTenantIdAndDateRange(
                TENANT_ID, rangeStart, rangeEnd, PageRequest.of(0, 20));

        // Then
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndDateRange_boundaryDates_inclusive() {
        // Given
        Instant rangeStart = baseTime;
        Instant rangeEnd = baseTime.plus(3, ChronoUnit.HOURS);
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.CREATE, "Product", "INVENTORY",
                rangeStart, true);
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.UPDATE, "Product", "INVENTORY",
                rangeEnd, true);

        // When
        Page<AuditLog> result = auditLogRepository.findByTenantIdAndDateRange(
                TENANT_ID, rangeStart, rangeEnd, PageRequest.of(0, 20));

        // Then
        assertEquals(2, result.getTotalElements());
    }

    // ---- countFailedLoginsSince ----

    @Test
    void countFailedLoginsSince_hasFailedLogins_returnsCorrectCount() {
        // Given
        Instant since = baseTime;
        for (int i = 0; i < 4; i++) {
            createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.LOGIN_FAILED, "User", "AUTH",
                    baseTime.plus(i + 1, ChronoUnit.HOURS), false);
        }
        for (int i = 0; i < 2; i++) {
            createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.LOGIN_FAILED, "User", "AUTH",
                    baseTime.minus(i + 1, ChronoUnit.HOURS), false);
        }

        // When
        long result = auditLogRepository.countFailedLoginsSince(TENANT_ID, since);

        // Then
        assertEquals(4, result);
    }

    @Test
    void countFailedLoginsSince_noFailedLogins_returnsZero() {
        // Given
        Instant since = baseTime;
        createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.LOGIN, "User", "AUTH",
                baseTime.plus(1, ChronoUnit.HOURS), true);

        // When
        long result = auditLogRepository.countFailedLoginsSince(TENANT_ID, since);

        // Then
        assertEquals(0, result);
    }

    @Test
    void countFailedLoginsSince_multiTenant_isolatedCount() {
        // Given — other tenant has failed logins, this tenant does not
        Instant since = baseTime;
        createLog(OTHER_TENANT_ID, 2L, "user2", AuditLog.AuditAction.LOGIN_FAILED, "User", "AUTH",
                baseTime.plus(1, ChronoUnit.HOURS), false);
        createLog(OTHER_TENANT_ID, 2L, "user2", AuditLog.AuditAction.LOGIN_FAILED, "User", "AUTH",
                baseTime.plus(2, ChronoUnit.HOURS), false);

        // When
        long result = auditLogRepository.countFailedLoginsSince(TENANT_ID, since);

        // Then
        assertEquals(0, result);
    }

    // ---- countModuleActivityByTenantIdSince ----

    @Test
    void countModuleActivityByTenantIdSince_multipleModules_returnsEntries() {
        // Given
        Instant since = baseTime;
        for (int i = 0; i < 6; i++) {
            createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.CREATE, "Product", "INVENTORY",
                    baseTime.plus(i + 1, ChronoUnit.MINUTES), true);
        }
        for (int i = 0; i < 2; i++) {
            createLog(TENANT_ID, 1L, "admin", AuditLog.AuditAction.CREATE, "Transaction", "POS",
                    baseTime.plus(i + 10, ChronoUnit.MINUTES), true);
        }

        // When
        List<Object[]> result = auditLogRepository.countModuleActivityByTenantIdSince(TENANT_ID, since);

        // Then
        assertEquals(2, result.size());
        assertEquals("INVENTORY", result.get(0)[0]);
        assertEquals(6L, result.get(0)[1]);
        assertEquals("POS", result.get(1)[0]);
        assertEquals(2L, result.get(1)[1]);
    }

    @Test
    void countModuleActivityByTenantIdSince_noActivity_returnsEmptyResult() {
        // Given — no logs after since
        Instant since = baseTime;

        // When
        List<Object[]> result = auditLogRepository.countModuleActivityByTenantIdSince(TENANT_ID, since);

        // Then
        assertTrue(result.isEmpty());
    }

    // ---- findMostActiveUsersByTenantIdSince ----

    @Test
    void findMostActiveUsersByTenantIdSince_multipleUsers_sortedByCountDesc() {
        // Given — User A: 8, User B: 2, User C: 5
        Instant since = baseTime;
        for (int i = 0; i < 8; i++) {
            createLog(TENANT_ID, 1L, "userA", AuditLog.AuditAction.CREATE, "Product", "INVENTORY",
                    baseTime.plus(i + 1, ChronoUnit.MINUTES), true);
        }
        for (int i = 0; i < 2; i++) {
            createLog(TENANT_ID, 2L, "userB", AuditLog.AuditAction.UPDATE, "Product", "INVENTORY",
                    baseTime.plus(i + 20, ChronoUnit.MINUTES), true);
        }
        for (int i = 0; i < 5; i++) {
            createLog(TENANT_ID, 3L, "userC", AuditLog.AuditAction.DELETE, "Product", "INVENTORY",
                    baseTime.plus(i + 30, ChronoUnit.MINUTES), true);
        }

        // When
        List<Object[]> result = auditLogRepository.findMostActiveUsersByTenantIdSince(TENANT_ID, since);

        // Then
        assertEquals(3, result.size());
        assertEquals("userA", result.get(0)[1]);
        assertEquals(8L, result.get(0)[2]);
        assertEquals("userC", result.get(1)[1]);
        assertEquals(5L, result.get(1)[2]);
        assertEquals("userB", result.get(2)[1]);
        assertEquals(2L, result.get(2)[2]);
    }

    @Test
    void findMostActiveUsersByTenantIdSince_noActivity_returnsEmptyList() {
        // Given — no logs after since
        Instant since = baseTime;

        // When
        List<Object[]> result = auditLogRepository.findMostActiveUsersByTenantIdSince(TENANT_ID, since);

        // Then
        assertTrue(result.isEmpty());
    }
}
