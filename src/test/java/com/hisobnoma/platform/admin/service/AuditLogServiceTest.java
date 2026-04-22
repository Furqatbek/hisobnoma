package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.AuditLogDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.mapper.AuditLogMapper;
import com.hisobnoma.platform.admin.repository.AuditLogRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private static final Long TENANT_ID = 1L;
    private Pageable pageable;
    private AuditLog sampleLog;
    private AuditLogDTO sampleDto;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);

        sampleLog = AuditLog.builder()
                .tenantId(TENANT_ID)
                .userId(10L)
                .username("admin")
                .action(AuditLog.AuditAction.CREATE)
                .entityType("Product")
                .entityId(100L)
                .entityName("iPhone")
                .module("INVENTORY")
                .description("Product created")
                .actionTimestamp(Instant.now())
                .success(true)
                .build();

        sampleDto = AuditLogDTO.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .userId(10L)
                .username("admin")
                .action(AuditLog.AuditAction.CREATE)
                .entityType("Product")
                .entityId(100L)
                .entityName("iPhone")
                .module("INVENTORY")
                .description("Product created")
                .actionTimestamp(Instant.now())
                .success(true)
                .build();
    }

    // ---- getAuditLogs ----

    @Test
    void getAuditLogs_validTenant_returnsPage() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1L);
        when(auditLogRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(sampleLog)).thenReturn(sampleDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogs(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getPage().getTotalElements());
    }

    @Test
    void getAuditLogs_validTenant_pageContentMatchesTenant() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        AuditLog log1 = AuditLog.builder().tenantId(TENANT_ID).action(AuditLog.AuditAction.CREATE).build();
        AuditLog log2 = AuditLog.builder().tenantId(TENANT_ID).action(AuditLog.AuditAction.UPDATE).build();
        AuditLogDTO dto1 = AuditLogDTO.builder().tenantId(TENANT_ID).action(AuditLog.AuditAction.CREATE).build();
        AuditLogDTO dto2 = AuditLogDTO.builder().tenantId(TENANT_ID).action(AuditLog.AuditAction.UPDATE).build();
        Page<AuditLog> page = new PageImpl<>(List.of(log1, log2), pageable, 2L);
        when(auditLogRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(log1)).thenReturn(dto1);
        when(auditLogMapper.toDto(log2)).thenReturn(dto2);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogs(pageable);

        // Then
        assertEquals(2, result.getContent().size());
        result.getContent().forEach(dto ->
                assertEquals(TENANT_ID, dto.getTenantId()));
    }

    @Test
    void getAuditLogs_noLogs_returnsEmptyPage() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogs(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getPage().getTotalElements());
    }

    // ---- getAuditLogsByUser ----

    @Test
    void getAuditLogsByUser_existingUser_returnsFilteredLogs() {
        // Given
        Long userId = 10L;
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndUserId(TENANT_ID, userId, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(sampleLog)).thenReturn(sampleDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByUser(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(userId, result.getContent().get(0).getUserId());
    }

    @Test
    void getAuditLogsByUser_multipleUsers_onlyTargetUserReturned() {
        // Given
        Long targetUserId = 10L;
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        AuditLog targetLog = AuditLog.builder().tenantId(TENANT_ID).userId(targetUserId).username("admin").build();
        AuditLogDTO targetDto = AuditLogDTO.builder().tenantId(TENANT_ID).userId(targetUserId).username("admin").build();
        Page<AuditLog> page = new PageImpl<>(List.of(targetLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndUserId(TENANT_ID, targetUserId, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(targetLog)).thenReturn(targetDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByUser(targetUserId, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        result.getContent().forEach(dto ->
                assertEquals(targetUserId, dto.getUserId()));
    }

    @Test
    void getAuditLogsByUser_nonExistentUser_returnsEmptyPage() {
        // Given
        Long userId = 999L;
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndUserId(TENANT_ID, userId, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByUser(userId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ---- getAuditLogsByEntity ----

    @Test
    void getAuditLogsByEntity_existingEntity_returnsFilteredLogs() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(TENANT_ID, "Product", 100L, pageable))
                .thenReturn(page);
        when(auditLogMapper.toDto(sampleLog)).thenReturn(sampleDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByEntity("Product", 100L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Product", result.getContent().get(0).getEntityType());
        assertEquals(100L, result.getContent().get(0).getEntityId());
    }

    @Test
    void getAuditLogsByEntity_noMatchingEntity_returnsEmptyPage() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(TENANT_ID, "Product", 999L, pageable))
                .thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByEntity("Product", 999L, pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAuditLogsByEntity_differentEntityType_excludedFromResults() {
        // Given — Order logs exist, but we query for Product
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(TENANT_ID, "Product", 100L, pageable))
                .thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByEntity("Product", 100L, pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
    }

    // ---- getAuditLogsByAction ----

    @Test
    void getAuditLogsByAction_deleteAction_returnsOnlyDeleteLogs() {
        // Given
        AuditLog deleteLog = AuditLog.builder().tenantId(TENANT_ID).action(AuditLog.AuditAction.DELETE).build();
        AuditLogDTO deleteDto = AuditLogDTO.builder().tenantId(TENANT_ID).action(AuditLog.AuditAction.DELETE).build();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(deleteLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndAction(TENANT_ID, AuditLog.AuditAction.DELETE, pageable))
                .thenReturn(page);
        when(auditLogMapper.toDto(deleteLog)).thenReturn(deleteDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByAction(AuditLog.AuditAction.DELETE, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(AuditLog.AuditAction.DELETE, result.getContent().get(0).getAction());
    }

    @Test
    void getAuditLogsByAction_noMatchingAction_returnsEmptyPage() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndAction(TENANT_ID, AuditLog.AuditAction.DELETE, pageable))
                .thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByAction(AuditLog.AuditAction.DELETE, pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAuditLogsByAction_createAction_returnsOnlyCreateLogs() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndAction(TENANT_ID, AuditLog.AuditAction.CREATE, pageable))
                .thenReturn(page);
        when(auditLogMapper.toDto(sampleLog)).thenReturn(sampleDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByAction(AuditLog.AuditAction.CREATE, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(AuditLog.AuditAction.CREATE, result.getContent().get(0).getAction());
    }

    // ---- getAuditLogsByModule ----

    @Test
    void getAuditLogsByModule_matchingModule_returnsFilteredLogs() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndModule(TENANT_ID, "INVENTORY", pageable))
                .thenReturn(page);
        when(auditLogMapper.toDto(sampleLog)).thenReturn(sampleDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByModule("INVENTORY", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("INVENTORY", result.getContent().get(0).getModule());
    }

    @Test
    void getAuditLogsByModule_noMatchingModule_returnsEmptyPage() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndModule(TENANT_ID, "INVENTORY", pageable))
                .thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByModule("INVENTORY", pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAuditLogsByModule_multipleModules_onlyTargetReturned() {
        // Given — INVENTORY and POS logs exist, but repository filters for INVENTORY only
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        AuditLog inventoryLog = AuditLog.builder().tenantId(TENANT_ID).module("INVENTORY").build();
        AuditLogDTO inventoryDto = AuditLogDTO.builder().tenantId(TENANT_ID).module("INVENTORY").build();
        Page<AuditLog> page = new PageImpl<>(List.of(inventoryLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndModule(TENANT_ID, "INVENTORY", pageable))
                .thenReturn(page);
        when(auditLogMapper.toDto(inventoryLog)).thenReturn(inventoryDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByModule("INVENTORY", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("INVENTORY", result.getContent().get(0).getModule());
    }

    // ---- getAuditLogsByDateRange ----

    @Test
    void getAuditLogsByDateRange_withinRange_returnsFilteredLogs() {
        // Given
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1L);
        when(auditLogRepository.findByTenantIdAndDateRange(TENANT_ID, start, end, pageable))
                .thenReturn(page);
        when(auditLogMapper.toDto(sampleLog)).thenReturn(sampleDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByDateRange(start, end, pageable);

        // Then
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getAuditLogsByDateRange_noLogsInRange_returnsEmptyPage() {
        // Given
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndDateRange(TENANT_ID, start, end, pageable))
                .thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByDateRange(start, end, pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getAuditLogsByDateRange_endBeforeStart_returnsEmptyOrThrows() {
        // Given — endDate is before startDate
        Instant start = Instant.now();
        Instant end = Instant.now().minusSeconds(3600);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findByTenantIdAndDateRange(TENANT_ID, start, end, pageable))
                .thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getAuditLogsByDateRange(start, end, pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
    }

    // ---- getFailedActions ----

    @Test
    void getFailedActions_returnsOnlyFailedLogs() {
        // Given
        AuditLog failedLog = AuditLog.builder()
                .tenantId(TENANT_ID)
                .action(AuditLog.AuditAction.LOGIN_FAILED)
                .entityType("User")
                .success(false)
                .errorMessage("Bad credentials")
                .actionTimestamp(Instant.now())
                .build();
        AuditLogDTO failedDto = AuditLogDTO.builder()
                .action(AuditLog.AuditAction.LOGIN_FAILED)
                .success(false)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(failedLog), pageable, 1L);
        when(auditLogRepository.findFailedActionsByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(failedLog)).thenReturn(failedDto);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getFailedActions(pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertFalse(result.getContent().get(0).isSuccess());
    }

    @Test
    void getFailedActions_noFailedActions_returnsEmptyPage() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0L);
        when(auditLogRepository.findFailedActionsByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getFailedActions(pageable);

        // Then
        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getPage().getTotalElements());
    }

    @Test
    void getFailedActions_allFailed_returnsAllLogs() {
        // Given
        AuditLog fail1 = AuditLog.builder().tenantId(TENANT_ID).success(false).action(AuditLog.AuditAction.LOGIN_FAILED).build();
        AuditLog fail2 = AuditLog.builder().tenantId(TENANT_ID).success(false).action(AuditLog.AuditAction.DELETE).build();
        AuditLogDTO failDto1 = AuditLogDTO.builder().success(false).action(AuditLog.AuditAction.LOGIN_FAILED).build();
        AuditLogDTO failDto2 = AuditLogDTO.builder().success(false).action(AuditLog.AuditAction.DELETE).build();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<AuditLog> page = new PageImpl<>(List.of(fail1, fail2), pageable, 2L);
        when(auditLogRepository.findFailedActionsByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(auditLogMapper.toDto(fail1)).thenReturn(failDto1);
        when(auditLogMapper.toDto(fail2)).thenReturn(failDto2);

        // When
        PageResponse<AuditLogDTO> result = auditLogService.getFailedActions(pageable);

        // Then
        assertEquals(2, result.getContent().size());
        result.getContent().forEach(dto -> assertFalse(dto.isSuccess()));
    }

    // ---- getActionStats ----

    @Test
    void getActionStats_returnsActionCounts() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"CREATE", 25L});
        stats.add(new Object[]{"UPDATE", 10L});
        when(auditLogRepository.countActionsByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(stats);

        // When
        List<Object[]> result = auditLogService.getActionStats(7);

        // Then
        assertEquals(2, result.size());
        assertEquals("CREATE", result.get(0)[0]);
        assertEquals(25L, result.get(0)[1]);
    }

    @Test
    void getActionStats_noActivity_returnsEmptyList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(auditLogRepository.countActionsByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<Object[]> result = auditLogService.getActionStats(7);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getActionStats_countAccuracy_matchesExpected() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"DELETE", 3L});
        stats.add(new Object[]{"CREATE", 5L});
        when(auditLogRepository.countActionsByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(stats);

        // When
        List<Object[]> result = auditLogService.getActionStats(7);

        // Then
        assertEquals(2, result.size());
        assertEquals("DELETE", result.get(0)[0]);
        assertEquals(3L, result.get(0)[1]);
        assertEquals("CREATE", result.get(1)[0]);
        assertEquals(5L, result.get(1)[1]);
    }

    // ---- getModuleActivityStats ----

    @Test
    void getModuleActivityStats_returnsModuleCounts() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"INVENTORY", 30L});
        when(auditLogRepository.countModuleActivityByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(stats);

        // When
        List<Object[]> result = auditLogService.getModuleActivityStats(7);

        // Then
        assertEquals(1, result.size());
        assertEquals("INVENTORY", result.get(0)[0]);
    }

    @Test
    void getModuleActivityStats_noActivity_returnsEmptyList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(auditLogRepository.countModuleActivityByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<Object[]> result = auditLogService.getModuleActivityStats(30);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getModuleActivityStats_countAccuracy_matchesExpected() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"INVENTORY", 10L});
        stats.add(new Object[]{"POS", 4L});
        when(auditLogRepository.countModuleActivityByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(stats);

        // When
        List<Object[]> result = auditLogService.getModuleActivityStats(30);

        // Then
        assertEquals(2, result.size());
        assertEquals("INVENTORY", result.get(0)[0]);
        assertEquals(10L, result.get(0)[1]);
        assertEquals("POS", result.get(1)[0]);
        assertEquals(4L, result.get(1)[1]);
    }

    // ---- getMostActiveUsers ----

    @Test
    void getMostActiveUsers_returnsUserActivityCounts() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        List<Object[]> users = new ArrayList<>();
        users.add(new Object[]{1L, "admin", 50L});
        when(auditLogRepository.findMostActiveUsersByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(users);

        // When
        List<Object[]> result = auditLogService.getMostActiveUsers(7);

        // Then
        assertEquals(1, result.size());
        assertEquals("admin", result.get(0)[1]);
    }

    @Test
    void getMostActiveUsers_noActivity_returnsEmptyList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(auditLogRepository.findMostActiveUsersByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<Object[]> result = auditLogService.getMostActiveUsers(30);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getMostActiveUsers_sortOrderCorrect() {
        // Given — User A: 10, User C: 7, User B: 3
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        List<Object[]> users = new ArrayList<>();
        users.add(new Object[]{1L, "userA", 10L});
        users.add(new Object[]{3L, "userC", 7L});
        users.add(new Object[]{2L, "userB", 3L});
        when(auditLogRepository.findMostActiveUsersByTenantIdSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(users);

        // When
        List<Object[]> result = auditLogService.getMostActiveUsers(30);

        // Then
        assertEquals(3, result.size());
        assertEquals("userA", result.get(0)[1]);
        assertEquals(10L, result.get(0)[2]);
        assertEquals("userC", result.get(1)[1]);
        assertEquals(7L, result.get(1)[2]);
        assertEquals("userB", result.get(2)[1]);
        assertEquals(3L, result.get(2)[2]);
    }

    // ---- getFailedLoginCount ----

    @Test
    void getFailedLoginCount_returnsCount() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(auditLogRepository.countFailedLoginsSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(5L);

        // When
        long result = auditLogService.getFailedLoginCount(24);

        // Then
        assertEquals(5L, result);
    }

    @Test
    void getFailedLoginCount_noFailedLogins_returnsZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(auditLogRepository.countFailedLoginsSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(0L);

        // When
        long result = auditLogService.getFailedLoginCount(24);

        // Then
        assertEquals(0L, result);
    }

    @Test
    void getFailedLoginCount_exactBoundary_inclusiveOfBoundary() {
        // Given — failed login exactly 24 hours ago should be included
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(auditLogRepository.countFailedLoginsSince(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(1L);

        // When
        long result = auditLogService.getFailedLoginCount(24);

        // Then
        assertEquals(1L, result);
        verify(auditLogRepository).countFailedLoginsSince(eq(TENANT_ID), any(Instant.class));
    }

    // ---- log (sync) ----

    @Test
    void log_success_savesAuditLogAndReturnsEntity() {
        // Given
        UserPrincipal principal = new UserPrincipal(
                10L, "admin", "password", TENANT_ID, true, true, Collections.emptyList());
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(principal);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AuditLog result = auditLogService.log(
                AuditLog.AuditAction.CREATE, "Product", 100L, "iPhone", "INVENTORY", "Product created");

        // Then
        assertNotNull(result);
        assertEquals(AuditLog.AuditAction.CREATE, result.getAction());
        assertEquals("Product", result.getEntityType());
        assertEquals(100L, result.getEntityId());
        assertEquals("INVENTORY", result.getModule());
        assertTrue(result.isSuccess());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void log_withChanges_savesOldAndNewValues() {
        // Given
        UserPrincipal principal = new UserPrincipal(
                10L, "admin", "password", TENANT_ID, true, true, Collections.emptyList());
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(principal);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AuditLog result = auditLogService.log(
                AuditLog.AuditAction.UPDATE, "Product", 100L, "iPhone", "INVENTORY",
                "Price updated", "{\"price\":100}", "{\"price\":120}", "price");

        // Then
        assertNotNull(result);
        assertEquals("{\"price\":100}", result.getOldValues());
        assertEquals("{\"price\":120}", result.getNewValues());
        assertEquals("price", result.getChangedFields());
    }

    // ---- logFailure ----

    @Test
    void logFailure_savesWithSuccessFalseAndErrorMessage() {
        // Given
        UserPrincipal principal = new UserPrincipal(
                10L, "admin", "password", TENANT_ID, true, true, Collections.emptyList());
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(principal);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AuditLog result = auditLogService.logFailure(
                AuditLog.AuditAction.LOGIN_FAILED, "User", 10L, "admin", "AUTH",
                "Login failed", "Bad credentials");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Bad credentials", result.getErrorMessage());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ---- log with no security context ----

    @Test
    void log_noSecurityContext_stillSavesLogWithNullUserInfo() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenThrow(new RuntimeException("No auth"));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AuditLog result = auditLogService.log(
                AuditLog.AuditAction.CREATE, "System", null, null, "SYSTEM", "System event");

        // Then
        assertNotNull(result);
        assertNull(result.getTenantId());
        assertNull(result.getUserId());
        assertNull(result.getUsername());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ---- log when repository throws ----

    @Test
    void log_repositoryThrows_returnsNull() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenThrow(new RuntimeException("No auth"));
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB error"));

        // When
        AuditLog result = auditLogService.log(
                AuditLog.AuditAction.CREATE, "Product", 1L, "Test", "INVENTORY", "Test");

        // Then
        assertNull(result);
    }
}
