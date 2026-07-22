package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.*;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.RefreshToken;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private Role adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Auth Tenant").code("FULLFLOW_AUTH").active(true)
                .maxUsers(100).maxLocations(10).build());

        Permission perm = permissionRepository.saveAndFlush(Permission.builder()
                .name("Admin User Manage").code("ADMIN_USER_MANAGE_AUTH")
                .module(Permission.Module.ADMIN).action(Permission.Action.MANAGE).build());

        adminRole = Role.builder()
                .name("Admin").code("ADMIN").tenantId(tenant.getId()).build();
        adminRole.getPermissions().add(perm);
        adminRole = roleRepository.saveAndFlush(adminRole);

        testUser = User.builder()
                .username("authflowuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Auth").lastName("User")
                .phone("+1234567890")
                .tenantId(tenant.getId()).enabled(true).build();
        testUser.getRoles().add(adminRole);
        testUser = userRepository.saveAndFlush(testUser);
    }

    private RequestPostProcessor userAuth() {
        UserPrincipal principal = new UserPrincipal(
                testUser.getId(), testUser.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("ADMIN_USER_MANAGE_AUTH")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- POST /api/v1/auth/login ----

    @Test
    void login_validCredentials_returnsTokensAndUserInfo() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("authflowuser").password("password123").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("authflowuser"))
                .andExpect(jsonPath("$.data.user.firstName").value("Auth"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("authflowuser").password("wrongpassword").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonExistentUser_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("nobody").password("password123").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        LoginRequest request = LoginRequest.builder().password("password123").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withRememberMe_returnsRememberMeFlag() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("authflowuser").password("password123").rememberMe(true).build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rememberMe").value(true));
    }

    // ---- POST /api/v1/auth/pin-login ----

    @Test
    void pinLogin_validPin_returnsTokens() throws Exception {
        testUser.setPinHash(passwordEncoder.encode("1234"));
        userRepository.saveAndFlush(testUser);

        PinLoginRequest request = PinLoginRequest.builder()
                .username("authflowuser").pin("1234").build();

        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void pinLogin_wrongPin_returns401() throws Exception {
        testUser.setPinHash(passwordEncoder.encode("1234"));
        userRepository.saveAndFlush(testUser);

        PinLoginRequest request = PinLoginRequest.builder()
                .username("authflowuser").pin("9999").build();

        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pinLogin_noPinSet_returns4xx() throws Exception {
        PinLoginRequest request = PinLoginRequest.builder()
                .username("authflowuser").pin("1234").build();

        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ---- GET /api/v1/auth/users/list ----

    @Test
    void getActiveUsers_withTenantHeader_returnsOnlyThatTenantsUsers() throws Exception {
        mockMvc.perform(get("/api/v1/auth/users/list")
                        .header("X-Tenant-ID", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].username").isNotEmpty());
    }

    @Test
    void getActiveUsers_withoutTenant_failsClosed() throws Exception {
        mockMvc.perform(get("/api/v1/auth/users/list"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getActiveUsers_otherTenant_doesNotSeeThisTenantsUsers() throws Exception {
        Tenant other = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Other Tenant").code("FULLFLOW_AUTH2").active(true)
                .maxUsers(100).maxLocations(10).build());

        mockMvc.perform(get("/api/v1/auth/users/list")
                        .header("X-Tenant-ID", other.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.username=='authflowuser')]").isEmpty());
    }

    // ---- POST /api/v1/auth/refresh ----

    @Test
    void refreshToken_validToken_returnsNewTokens() throws Exception {
        // First login to get a real refresh token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("authflowuser").password("password123").build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Now refresh
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken).build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void refreshToken_invalidToken_returns401() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token-xyz").build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/auth/logout ----

    @Test
    void logout_authenticated_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /api/v1/auth/me ----

    @Test
    void getCurrentUser_authenticated_returnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("authflowuser"))
                .andExpect(jsonPath("$.data.firstName").value("Auth"));
    }

    @Test
    void getCurrentUser_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }

    // ---- PUT /api/v1/auth/set-pin ----

    @Test
    void setPin_authenticated_validPin_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/auth/set-pin")
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "5678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void setPin_tooShort_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/set-pin")
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "12"))))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/auth/change-password ----

    @Test
    void changePassword_validRequest_returns200() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("password123")
                .newPassword("newPassword456")
                .confirmPassword("newPassword456")
                .build();

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changePassword_wrongCurrentPassword_returns4xx() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongcurrent")
                .newPassword("newPassword456")
                .confirmPassword("newPassword456")
                .build();

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void changePassword_passwordMismatch_returns4xx() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("password123")
                .newPassword("newPassword456")
                .confirmPassword("differentPassword")
                .build();

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .with(userAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ---- POST /api/v1/auth/forgot-password ----

    @Test
    void forgotPassword_existingUser_returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("authflowuser");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_nonExistentUser_stillReturns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nobody@nowhere.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- POST /api/v1/auth/reset-password ----

    @Test
    void resetPassword_invalidToken_returns4xx() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("bad-token")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ---- Full login flow ----

    @Test
    void fullLoginFlow_loginRefreshLogout() throws Exception {
        // Login
        LoginRequest loginRequest = LoginRequest.builder()
                .username("authflowuser").password("password123").build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Refresh
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken).build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Old refresh token should be revoked (rotation)
        RefreshTokenRequest oldRefreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken).build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldRefreshRequest)))
                .andExpect(status().isUnauthorized());

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout").with(userAuth()))
                .andExpect(status().isOk());
    }
}
