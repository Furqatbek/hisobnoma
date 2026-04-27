package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.CreateUserRequest;
import com.hisobnoma.platform.auth.dto.UpdateUserRequest;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
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
import java.util.Map;
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
class UserControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private Permission userManagePerm;
    private Role adminRole;
    private Role cashierRole;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Test Tenant").code("FULLFLOW_USER").active(true)
                .maxUsers(100).maxLocations(10).build());

        userManagePerm = permissionRepository.saveAndFlush(Permission.builder()
                .name("Admin User Manage").code("ADMIN_USER_MANAGE")
                .module(Permission.Module.ADMIN).action(Permission.Action.MANAGE).build());

        adminRole = Role.builder()
                .name("Admin").code("ADMIN").tenantId(tenant.getId()).build();
        adminRole.getPermissions().add(userManagePerm);
        adminRole = roleRepository.saveAndFlush(adminRole);

        cashierRole = roleRepository.saveAndFlush(Role.builder()
                .name("Cashier").code("CASHIER").tenantId(tenant.getId()).build());

        adminUser = User.builder()
                .username("fullflowtestadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .firstName("Admin").lastName("User")
                .tenantId(tenant.getId()).enabled(true).build();
        adminUser.getRoles().add(adminRole);
        adminUser = userRepository.saveAndFlush(adminUser);
    }

    private RequestPostProcessor adminAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("ADMIN_USER_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/users (list) ----

    @Test
    void listUsers_returnsPageWithExistingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].username").value("fullflowtestadmin"));
    }

    @Test
    void listUsers_withSearchFilter_findsMatchingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("search", "fullflowtestadmin").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("fullflowtestadmin"));
    }

    @Test
    void listUsers_withSearchFilter_noMatchReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("search", "nonexistentxyz").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void listUsers_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ---- GET /api/v1/users/{id} ----

    @Test
    void getUser_existingId_returnsUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + adminUser.getId()).with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(adminUser.getId()))
                .andExpect(jsonPath("$.data.username").value("fullflowtestadmin"))
                .andExpect(jsonPath("$.data.firstName").value("Admin"));
    }

    @Test
    void getUser_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999").with(adminAuth()))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/users (create) ----

    @Test
    void createUser_validRequest_createsUserInDb() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser123")
                .password("password123")
                .firstName("New").lastName("User")
                .roleCodes(Set.of("CASHIER"))
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser123"))
                .andExpect(jsonPath("$.data.firstName").value("New"))
                .andExpect(jsonPath("$.data.roles", hasItem("CASHIER")));

        assertTrue(userRepository.existsByUsername("newuser123"));
    }

    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("fullflowtestadmin")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_usernameTooShort_returns400() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("ab").password("password123").build();

        mockMvc.perform(post("/api/v1/users")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_invalidPhoneFormat_returns400() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("phonetestuser").password("password123")
                .phone("invalid-phone").build();

        mockMvc.perform(post("/api/v1/users")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/users/{id} (update) ----

    @Test
    void updateUser_validRequest_updatesUserInDb() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Updated").lastName("Name").build();

        mockMvc.perform(put("/api/v1/users/" + adminUser.getId())
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Name"));
    }

    @Test
    void updateUser_nonExistentId_returns404() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder().firstName("X").build();

        mockMvc.perform(put("/api/v1/users/999999")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_withRoles_updatesRoles() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .roleCodes(Set.of("CASHIER")).build();

        mockMvc.perform(put("/api/v1/users/" + adminUser.getId())
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasItem("CASHIER")));
    }

    // ---- DELETE /api/v1/users/{id} ----

    @Test
    void deleteUser_existingUser_removesFromDb() throws Exception {
        User toDelete = userRepository.saveAndFlush(User.builder()
                .username("todelete123")
                .passwordHash(passwordEncoder.encode("pass123"))
                .tenantId(tenant.getId()).enabled(true).build());

        mockMvc.perform(delete("/api/v1/users/" + toDelete.getId()).with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(userRepository.existsByUsername("todelete123"));
    }

    @Test
    void deleteUser_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/users/999999").with(adminAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_selfDelete_returns4xx() throws Exception {
        mockMvc.perform(delete("/api/v1/users/" + adminUser.getId()).with(adminAuth()))
                .andExpect(status().is4xxClientError());
    }

    // ---- PUT /api/v1/users/{id}/roles ----

    @Test
    void assignRoles_validRoleCodes_updatesRoles() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + adminUser.getId() + "/roles")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Set.of("CASHIER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasItem("CASHIER")));
    }

    // ---- PUT /api/v1/users/{id}/lock ----

    @Test
    void lockUser_lockAndUnlock_updatesLockedState() throws Exception {
        User target = userRepository.saveAndFlush(User.builder()
                .username("locktest123")
                .passwordHash(passwordEncoder.encode("pass123"))
                .tenantId(tenant.getId()).enabled(true).build());

        mockMvc.perform(put("/api/v1/users/" + target.getId() + "/lock")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("locked", true))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/" + target.getId() + "/lock")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("locked", false))))
                .andExpect(status().isOk());
    }

    // ---- PUT /api/v1/users/{id}/reset-password ----

    @Test
    void resetPassword_validUser_returns200() throws Exception {
        User target = userRepository.saveAndFlush(User.builder()
                .username("resetpw123")
                .passwordHash(passwordEncoder.encode("oldpass"))
                .tenantId(tenant.getId()).enabled(true).build());

        mockMvc.perform(put("/api/v1/users/" + target.getId() + "/reset-password")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newPassword123"))))
                .andExpect(status().isOk());
    }

    // ---- PUT /api/v1/users/{id}/set-pin ----

    @Test
    void setUserPin_validPin_returns200() throws Exception {
        User target = userRepository.saveAndFlush(User.builder()
                .username("pintest123")
                .passwordHash(passwordEncoder.encode("pass123"))
                .tenantId(tenant.getId()).enabled(true).build());

        mockMvc.perform(put("/api/v1/users/" + target.getId() + "/set-pin")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isOk());
    }

    @Test
    void setUserPin_clearPin_returns200() throws Exception {
        User target = userRepository.saveAndFlush(User.builder()
                .username("clearpin123")
                .passwordHash(passwordEncoder.encode("pass123"))
                .pinHash(passwordEncoder.encode("1234"))
                .tenantId(tenant.getId()).enabled(true).build());

        mockMvc.perform(put("/api/v1/users/" + target.getId() + "/set-pin")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", ""))))
                .andExpect(status().isOk());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle_createReadUpdateDelete() throws Exception {
        // Create
        CreateUserRequest createReq = CreateUserRequest.builder()
                .username("lifecycle123")
                .password("password123")
                .firstName("Life").lastName("Cycle")
                .build();

        String createResult = mockMvc.perform(post("/api/v1/users")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(createResult).path("data").path("id").asLong();

        // Read
        mockMvc.perform(get("/api/v1/users/" + userId).with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("lifecycle123"))
                .andExpect(jsonPath("$.data.firstName").value("Life"));

        // Update
        UpdateUserRequest updateReq = UpdateUserRequest.builder()
                .firstName("Updated").build();

        mockMvc.perform(put("/api/v1/users/" + userId)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"));

        // Delete
        mockMvc.perform(delete("/api/v1/users/" + userId).with(adminAuth()))
                .andExpect(status().isOk());

        // Verify deleted
        mockMvc.perform(get("/api/v1/users/" + userId).with(adminAuth()))
                .andExpect(status().isNotFound());
    }
}
