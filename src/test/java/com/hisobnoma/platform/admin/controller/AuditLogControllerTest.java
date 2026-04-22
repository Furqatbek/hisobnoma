package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.AuditLogDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.service.AuditLogService;
import com.hisobnoma.platform.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    private PageResponse<AuditLogDTO> samplePage() {
        AuditLogDTO dto = AuditLogDTO.builder()
                .id(1L)
                .tenantId(1L)
                .userId(10L)
                .username("admin")
                .action(AuditLog.AuditAction.CREATE)
                .entityType("Product")
                .entityId(100L)
                .module("INVENTORY")
                .actionTimestamp(Instant.now())
                .success(true)
                .build();
        return PageResponse.<AuditLogDTO>builder()
                .content(List.of(dto))
                .page(PageResponse.PageMetadata.builder()
                        .number(0).size(20).totalElements(1).totalPages(1)
                        .first(true).last(true).empty(false).build())
                .build();
    }

    private PageResponse<AuditLogDTO> emptyPage() {
        return PageResponse.<AuditLogDTO>builder()
                .content(Collections.emptyList())
                .page(PageResponse.PageMetadata.builder()
                        .number(0).size(20).totalElements(0).totalPages(0)
                        .first(true).last(true).empty(true).build())
                .build();
    }

    // ---- GET /api/v1/admin/audit-logs ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogs_adminRead_returns200PaginatedList() throws Exception {
        // Given
        when(auditLogService.getAuditLogs(any())).thenReturn(samplePage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAuditLogs_noAdminReadPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /api/v1/admin/audit-logs/user/{userId} ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByUser_existingUser_returns200FilteredList() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByUser(eq(10L), any())).thenReturn(samplePage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userId").value(10));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByUser_nonExistentUser_returns200EmptyPage() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByUser(eq(999L), any())).thenReturn(emptyPage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ---- GET /api/v1/admin/audit-logs/entity/{entityType}/{entityId} ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByEntity_validEntityTypeAndId_returns200() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByEntity(eq("Product"), eq(100L), any())).thenReturn(samplePage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/entity/Product/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].entityType").value("Product"))
                .andExpect(jsonPath("$.data.content[0].entityId").value(100));
    }

    // ---- GET /api/v1/admin/audit-logs/action/{action} ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByAction_validAction_returns200Filtered() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByAction(eq(AuditLog.AuditAction.CREATE), any())).thenReturn(samplePage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/action/CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].action").value("CREATE"));
    }

    // ---- GET /api/v1/admin/audit-logs/module/{module} ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByModule_validModule_returns200Filtered() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByModule(eq("INVENTORY"), any())).thenReturn(samplePage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/module/INVENTORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].module").value("INVENTORY"));
    }

    // ---- GET /api/v1/admin/audit-logs/date-range ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByDateRange_validDates_returns200Filtered() throws Exception {
        // Given
        when(auditLogService.getAuditLogsByDateRange(any(Instant.class), any(Instant.class), any()))
                .thenReturn(samplePage());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/date-range")
                        .param("startDate", "2026-01-01T00:00:00Z")
                        .param("endDate", "2026-12-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getAuditLogsByDateRange_invalidDateFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/date-range")
                        .param("startDate", "notadate")
                        .param("endDate", "alsonotadate"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /api/v1/admin/audit-logs/failed ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getFailedActions_adminRead_returns200OnlyFailed() throws Exception {
        // Given
        AuditLogDTO failedDto = AuditLogDTO.builder()
                .id(2L).action(AuditLog.AuditAction.LOGIN_FAILED).success(false).build();
        PageResponse<AuditLogDTO> failedPage = PageResponse.<AuditLogDTO>builder()
                .content(List.of(failedDto))
                .page(PageResponse.PageMetadata.builder()
                        .number(0).size(20).totalElements(1).totalPages(1)
                        .first(true).last(true).empty(false).build())
                .build();
        when(auditLogService.getFailedActions(any())).thenReturn(failedPage);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].success").value(false));
    }

    // ---- GET /api/v1/admin/audit-logs/stats/actions ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getActionStats_sevenDays_returns200() throws Exception {
        // Given
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"CREATE", 25L});
        stats.add(new Object[]{"DELETE", 3L});
        when(auditLogService.getActionStats(7)).thenReturn(stats);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/actions").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/audit-logs/stats/modules ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getModuleStats_thirtyDays_returns200() throws Exception {
        // Given
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"INVENTORY", 10L});
        when(auditLogService.getModuleActivityStats(30)).thenReturn(stats);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/modules").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/audit-logs/stats/users ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getMostActiveUsers_thirtyDays_returns200List() throws Exception {
        // Given
        List<Object[]> users = new ArrayList<>();
        users.add(new Object[]{1L, "admin", 50L});
        when(auditLogService.getMostActiveUsers(30)).thenReturn(users);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/users").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/audit-logs/stats/failed-logins ----

    @Test
    @WithMockUser(authorities = "ADMIN_AUDIT_VIEW")
    void getFailedLoginCount_24Hours_returns200Integer() throws Exception {
        // Given
        when(auditLogService.getFailedLoginCount(24)).thenReturn(5L);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/failed-logins").param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(5));
    }
}
