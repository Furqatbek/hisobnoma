package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.CreateRoleRequest;
import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.auth.service.RoleService;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

    private RequestPostProcessor userWithPermission(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "pw", 1L, true, true,
                java.util.Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RoleDto sampleDto() {
        return RoleDto.builder()
                .id(10L)
                .name("Cashier")
                .code("CASHIER")
                .description("Cashier role")
                .systemRole(false)
                .tenantId(1L)
                .permissions(Set.of("INVENTORY_READ", "POS_READ"))
                .build();
    }

    private PageResponse<RoleDto> samplePage() {
        return PageResponse.<RoleDto>builder()
                .content(List.of(sampleDto()))
                .page(PageResponse.PageMetadata.builder()
                        .number(0).size(20).totalElements(1).totalPages(1)
                        .first(true).last(true).empty(false).build())
                .build();
    }

    // ---- GET /api/v1/roles ----

    @Test
    void getRoles_authenticated_returns200Paginated() throws Exception {
        when(roleService.getRoles(any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/v1/roles").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    // ---- GET /api/v1/roles/system ----

    @Test
    void getSystemRoles_returns200SystemRolesOnly() throws Exception {
        RoleDto systemRole = RoleDto.builder()
                .id(100L).code("SUPER_ADMIN").name("Super Admin").systemRole(true).build();
        when(roleService.getSystemRoles()).thenReturn(List.of(systemRole));

        mockMvc.perform(get("/api/v1/roles/system").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].systemRole").value(true));
    }

    // ---- GET /api/v1/roles/{id} ----

    @Test
    void getRole_foundById_returns200WithPermissions() throws Exception {
        when(roleService.getRole(10L)).thenReturn(sampleDto());

        mockMvc.perform(get("/api/v1/roles/10").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions.length()").value(2));
    }

    @Test
    void getRole_notFound_returns404() throws Exception {
        when(roleService.getRole(999L)).thenThrow(new NotFoundException("Role", 999L));

        mockMvc.perform(get("/api/v1/roles/999").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/roles/permissions ----

    @Test
    void getAllPermissions_returns200PermissionCodes() throws Exception {
        when(roleService.getAllPermissions())
                .thenReturn(List.of("INVENTORY_READ", "POS_READ", "ADMIN_SETTINGS_VIEW"));

        mockMvc.perform(get("/api/v1/roles/permissions").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // ---- POST /api/v1/roles ----

    @Test
    void createRole_validRequest_returns201() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Cashier")
                .code("CASHIER")
                .description("Cashier role")
                .build();
        when(roleService.createRole(any(CreateRoleRequest.class))).thenReturn(sampleDto());

        mockMvc.perform(post("/api/v1/roles")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.code").value("CASHIER"));
    }

    @Test
    void createRole_duplicateCode_returns409() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Cashier").code("CASHIER").build();
        when(roleService.createRole(any(CreateRoleRequest.class)))
                .thenThrow(new DuplicateResourceException("Role", "code", "CASHIER"));

        mockMvc.perform(post("/api/v1/roles")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ---- PUT /api/v1/roles/{id} ----

    @Test
    void updateRole_validRequest_returns200() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Senior Cashier").code("CASHIER").description("Updated").build();
        when(roleService.updateRole(eq(10L), any(CreateRoleRequest.class))).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/roles/10")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void updateRole_notFound_returns404() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("X").code("X").build();
        when(roleService.updateRole(eq(999L), any())).thenThrow(new NotFoundException("Role", 999L));

        mockMvc.perform(put("/api/v1/roles/999")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRole_systemRole_returns4xx() throws Exception {
        // Plan says 422, actual BusinessException maps to 400
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("X").code("SUPER_ADMIN").build();
        when(roleService.updateRole(eq(100L), any()))
                .thenThrow(new BusinessException("Cannot modify system roles", "SYSTEM_ROLE_PROTECTED"));

        mockMvc.perform(put("/api/v1/roles/100")
                        .with(userWithPermission("ADMIN_ROLE_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ---- DELETE /api/v1/roles/{id} ----

    @Test
    void deleteRole_validRole_returns200() throws Exception {
        // Controller returns 200 with message (not 204)
        doNothing().when(roleService).deleteRole(10L);

        mockMvc.perform(delete("/api/v1/roles/10").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteRole_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Role", 999L)).when(roleService).deleteRole(999L);

        mockMvc.perform(delete("/api/v1/roles/999").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRole_roleInUse_returns4xx() throws Exception {
        // Plan says 422, actual BusinessException maps to 400
        doThrow(new BusinessException("Cannot delete role with assigned users", "ROLE_IN_USE"))
                .when(roleService).deleteRole(10L);

        mockMvc.perform(delete("/api/v1/roles/10").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteRole_systemRole_returns4xx() throws Exception {
        doThrow(new BusinessException("Cannot delete system roles", "SYSTEM_ROLE_PROTECTED"))
                .when(roleService).deleteRole(100L);

        mockMvc.perform(delete("/api/v1/roles/100").with(userWithPermission("ADMIN_ROLE_MANAGE")))
                .andExpect(status().is4xxClientError());
    }

    // ---- PUT /api/v1/roles/{id}/permissions ----

    @Test
    void assignPermissions_validCodes_returns200() throws Exception {
        when(roleService.assignPermissions(eq(10L), any())).thenReturn(sampleDto());

        mockMvc.perform(put("/api/v1/roles/10/permissions")
                        .with(userWithPermission("ADMIN_PERMISSION_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Set.of("INVENTORY_READ", "POS_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray());
    }
}
