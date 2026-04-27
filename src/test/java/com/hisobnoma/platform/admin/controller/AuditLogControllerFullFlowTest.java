package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.repository.AuditLogRepository;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("AuditLog Tenant").code("FULLFLOW_AUDIT").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("auditlogadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        Instant now = Instant.now();

        auditLogRepository.saveAndFlush(AuditLog.builder()
                .tenantId(tenant.getId())
                .userId(adminUser.getId()).username("auditlogadmin").userFullName("Admin User")
                .action(AuditLog.AuditAction.CREATE)
                .entityType("User").entityId(100L).entityName("New User")
                .module("AUTH").description("Created a new user")
                .actionTimestamp(now).success(true).build());

        auditLogRepository.saveAndFlush(AuditLog.builder()
                .tenantId(tenant.getId())
                .userId(adminUser.getId()).username("auditlogadmin").userFullName("Admin User")
                .action(AuditLog.AuditAction.UPDATE)
                .entityType("Role").entityId(200L).entityName("Cashier Role")
                .module("AUTH").description("Updated role permissions")
                .actionTimestamp(now.minus(1, ChronoUnit.HOURS)).success(true).build());

        auditLogRepository.saveAndFlush(AuditLog.builder()
                .tenantId(tenant.getId())
                .userId(adminUser.getId()).username("auditlogadmin").userFullName("Admin User")
                .action(AuditLog.AuditAction.LOGIN_FAILED)
                .entityType("User").entityId(adminUser.getId()).entityName("auditlogadmin")
                .module("AUTH").description("Failed login attempt")
                .actionTimestamp(now.minus(30, ChronoUnit.MINUTES))
                .success(false).errorMessage("Invalid credentials").build());

        auditLogRepository.saveAndFlush(AuditLog.builder()
                .tenantId(tenant.getId())
                .userId(adminUser.getId()).username("auditlogadmin").userFullName("Admin User")
                .action(AuditLog.AuditAction.DELETE)
                .entityType("Product").entityId(300L).entityName("Old Product")
                .module("INVENTORY").description("Deleted product")
                .actionTimestamp(now.minus(2, ChronoUnit.HOURS)).success(true).build());
    }

    private RequestPostProcessor auditAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("ADMIN_AUDIT_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/admin/audit-logs ----

    @Test
    void getAuditLogs_returnsPaginatedLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs").with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(4))
                .andExpect(jsonPath("$.data.page.totalElements").value(4));
    }

    @Test
    void getAuditLogs_withPagination_respectsPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("size", "2").param("page", "0")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(4));
    }

    // ---- GET /api/v1/admin/audit-logs/user/{userId} ----

    @Test
    void getAuditLogsByUser_returnsUserLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/user/" + adminUser.getId())
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(4));
    }

    @Test
    void getAuditLogsByUser_nonExistentUser_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/user/999999")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ---- GET /api/v1/admin/audit-logs/entity/{entityType}/{entityId} ----

    @Test
    void getAuditLogsByEntity_returnsEntityLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/entity/User/100")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].entityType").value("User"));
    }

    // ---- GET /api/v1/admin/audit-logs/action/{action} ----

    @Test
    void getAuditLogsByAction_returnsFilteredLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/action/CREATE")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    // ---- GET /api/v1/admin/audit-logs/module/{module} ----

    @Test
    void getAuditLogsByModule_returnsModuleLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/module/AUTH")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3));
    }

    @Test
    void getAuditLogsByModule_inventoryModule_returnsInventoryLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/module/INVENTORY")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    // ---- GET /api/v1/admin/audit-logs/date-range ----

    @Test
    void getAuditLogsByDateRange_withinRange_returnsLogs() throws Exception {
        String start = Instant.now().minus(3, ChronoUnit.HOURS).toString();
        String end = Instant.now().plus(1, ChronoUnit.HOURS).toString();

        mockMvc.perform(get("/api/v1/admin/audit-logs/date-range")
                        .param("startDate", start).param("endDate", end)
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(4));
    }

    @Test
    void getAuditLogsByDateRange_outsideRange_returnsEmpty() throws Exception {
        String start = Instant.now().minus(10, ChronoUnit.DAYS).toString();
        String end = Instant.now().minus(9, ChronoUnit.DAYS).toString();

        mockMvc.perform(get("/api/v1/admin/audit-logs/date-range")
                        .param("startDate", start).param("endDate", end)
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ---- GET /api/v1/admin/audit-logs/failed ----

    @Test
    void getFailedActions_returnsOnlyFailedLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/failed").with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].success").value(false));
    }

    // ---- GET /api/v1/admin/audit-logs/stats/actions ----

    @Test
    void getActionStats_returnsStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/actions")
                        .param("days", "7")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/audit-logs/stats/modules ----

    @Test
    void getModuleStats_returnsModuleActivity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/modules")
                        .param("days", "7")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/audit-logs/stats/users ----

    @Test
    void getMostActiveUsers_returnsUserActivity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/users")
                        .param("days", "7")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/audit-logs/stats/failed-logins ----

    @Test
    void getFailedLoginCount_returnsCount() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/stats/failed-logins")
                        .param("hours", "24")
                        .with(auditAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").isNumber());
    }

    // ---- Permission checks ----

    @Test
    void getAuditLogs_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/audit-logs").with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLogs_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
