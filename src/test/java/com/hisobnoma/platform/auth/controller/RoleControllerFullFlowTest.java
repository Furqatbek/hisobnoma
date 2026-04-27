package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.CreateRoleRequest;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private Permission roleManagePerm;
    private Permission permManagePerm;
    private Permission inventoryReadPerm;
    private Permission posReadPerm;
    private Role adminRole;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Test Tenant").code("FULLFLOW_ROLE").active(true)
                .maxUsers(100).maxLocations(10).build());

        roleManagePerm = permissionRepository.saveAndFlush(Permission.builder()
                .name("Admin Role Manage").code("ADMIN_ROLE_MANAGE")
                .module(Permission.Module.ADMIN).action(Permission.Action.MANAGE).build());

        permManagePerm = permissionRepository.saveAndFlush(Permission.builder()
                .name("Admin Permission Manage").code("ADMIN_PERMISSION_MANAGE")
                .module(Permission.Module.ADMIN).action(Permission.Action.MANAGE).build());

        inventoryReadPerm = permissionRepository.saveAndFlush(Permission.builder()
                .name("Inventory Read").code("INVENTORY_READ")
                .module(Permission.Module.INVENTORY).action(Permission.Action.READ).build());

        posReadPerm = permissionRepository.saveAndFlush(Permission.builder()
                .name("POS Read").code("POS_READ")
                .module(Permission.Module.POS).action(Permission.Action.READ).build());

        adminRole = Role.builder()
                .name("Admin").code("ADMIN").tenantId(tenant.getId()).build();
        adminRole.getPermissions().add(roleManagePerm);
        adminRole.getPermissions().add(permManagePerm);
        adminRole = roleRepository.saveAndFlush(adminRole);

        adminUser = User.builder()
                .username("roleflowadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build();
        adminUser.getRoles().add(adminRole);
        adminUser = userRepository.saveAndFlush(adminUser);
    }

    private RequestPostProcessor roleManageAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("ADMIN_ROLE_MANAGE"),
                        new SimpleGrantedAuthority("ADMIN_PERMISSION_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/roles ----

    @Test
    void listRoles_returnsPaginatedRoles() throws Exception {
        mockMvc.perform(get("/api/v1/roles").with(roleManageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(greaterThanOrEqualTo(1)));
    }

    // ---- GET /api/v1/roles/system ----

    @Test
    void getSystemRoles_returnsSystemRolesOnly() throws Exception {
        roleRepository.saveAndFlush(Role.builder()
                .name("Super Admin").code("SUPER_ADMIN").systemRole(true).build());

        mockMvc.perform(get("/api/v1/roles/system").with(roleManageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/roles/{id} ----

    @Test
    void getRole_existingId_returnsRoleWithPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/roles/" + adminRole.getId()).with(roleManageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(adminRole.getId()))
                .andExpect(jsonPath("$.data.code").value("ADMIN"))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void getRole_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/roles/999999").with(roleManageAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/roles/permissions ----

    @Test
    void getAllPermissions_returnsPermissionCodes() throws Exception {
        mockMvc.perform(get("/api/v1/roles/permissions").with(roleManageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(4)));
    }

    // ---- POST /api/v1/roles ----

    @Test
    void createRole_validRequest_createsRoleInDb() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Manager").code("MANAGER")
                .description("Manager role")
                .permissionCodes(Set.of("INVENTORY_READ", "POS_READ"))
                .build();

        mockMvc.perform(post("/api/v1/roles")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("MANAGER"))
                .andExpect(jsonPath("$.data.permissions", hasItems("INVENTORY_READ", "POS_READ")));

        assertTrue(roleRepository.existsByCodeAndTenantId("MANAGER", tenant.getId()));
    }

    @Test
    void createRole_duplicateCode_returns409() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Admin Dup").code("ADMIN").build();

        mockMvc.perform(post("/api/v1/roles")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createRole_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/roles")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_invalidCodeFormat_returns400() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Bad Code").code("lower_case").build();

        mockMvc.perform(post("/api/v1/roles")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/roles/{id} ----

    @Test
    void updateRole_validRequest_updatesInDb() throws Exception {
        Role toUpdate = roleRepository.saveAndFlush(Role.builder()
                .name("Temp").code("TEMP_ROLE").tenantId(tenant.getId()).build());

        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Updated Temp").code("TEMP_ROLE")
                .description("Updated description").build();

        mockMvc.perform(put("/api/v1/roles/" + toUpdate.getId())
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Temp"));
    }

    @Test
    void updateRole_systemRole_returns4xx() throws Exception {
        Role sysRole = roleRepository.saveAndFlush(Role.builder()
                .name("System").code("SYS_ROLE").systemRole(true).build());

        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Hacked").code("SYS_ROLE").build();

        mockMvc.perform(put("/api/v1/roles/" + sysRole.getId())
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateRole_nonExistentId_returns404() throws Exception {
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("X").code("X").build();

        mockMvc.perform(put("/api/v1/roles/999999")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/v1/roles/{id} ----

    @Test
    void deleteRole_noAssignedUsers_deletesFromDb() throws Exception {
        Role toDelete = roleRepository.saveAndFlush(Role.builder()
                .name("ToDelete").code("TO_DELETE").tenantId(tenant.getId()).build());

        mockMvc.perform(delete("/api/v1/roles/" + toDelete.getId()).with(roleManageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteRole_withAssignedUsers_returns4xx() throws Exception {
        // Clear Hibernate L1 cache so role.getUsers() lazy-loads from DB
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/roles/" + adminRole.getId()).with(roleManageAuth()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteRole_systemRole_returns4xx() throws Exception {
        Role sysRole = roleRepository.saveAndFlush(Role.builder()
                .name("System").code("SYS_DEL").systemRole(true).build());

        mockMvc.perform(delete("/api/v1/roles/" + sysRole.getId()).with(roleManageAuth()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteRole_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/999999").with(roleManageAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/roles/{id}/permissions ----

    @Test
    void assignPermissions_validCodes_updatesPermissions() throws Exception {
        Role target = roleRepository.saveAndFlush(Role.builder()
                .name("PermTest").code("PERM_TEST").tenantId(tenant.getId()).build());

        mockMvc.perform(put("/api/v1/roles/" + target.getId() + "/permissions")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Set.of("INVENTORY_READ", "POS_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", hasItems("INVENTORY_READ", "POS_READ")));
    }

    @Test
    void assignPermissions_systemRole_returns4xx() throws Exception {
        Role sysRole = roleRepository.saveAndFlush(Role.builder()
                .name("System").code("SYS_PERM").systemRole(true).build());

        mockMvc.perform(put("/api/v1/roles/" + sysRole.getId() + "/permissions")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Set.of("INVENTORY_READ"))))
                .andExpect(status().is4xxClientError());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle_createReadUpdateDelete() throws Exception {
        // Create
        CreateRoleRequest createReq = CreateRoleRequest.builder()
                .name("Lifecycle Role").code("LIFECYCLE_ROLE")
                .description("Test lifecycle")
                .permissionCodes(Set.of("INVENTORY_READ"))
                .build();

        String createResult = mockMvc.perform(post("/api/v1/roles")
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long roleId = objectMapper.readTree(createResult).path("data").path("id").asLong();

        // Read
        mockMvc.perform(get("/api/v1/roles/" + roleId).with(roleManageAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("LIFECYCLE_ROLE"))
                .andExpect(jsonPath("$.data.permissions", hasItem("INVENTORY_READ")));

        // Update
        CreateRoleRequest updateReq = CreateRoleRequest.builder()
                .name("Updated Lifecycle").code("LIFECYCLE_ROLE")
                .permissionCodes(Set.of("INVENTORY_READ", "POS_READ"))
                .build();

        mockMvc.perform(put("/api/v1/roles/" + roleId)
                        .with(roleManageAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Lifecycle"))
                .andExpect(jsonPath("$.data.permissions.length()").value(2));

        // Delete
        mockMvc.perform(delete("/api/v1/roles/" + roleId).with(roleManageAuth()))
                .andExpect(status().isOk());

        // Verify deleted
        mockMvc.perform(get("/api/v1/roles/" + roleId).with(roleManageAuth()))
                .andExpect(status().isNotFound());
    }
}
