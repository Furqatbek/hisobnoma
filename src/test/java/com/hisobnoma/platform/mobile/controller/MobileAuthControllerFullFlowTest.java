package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.entity.DeviceToken;
import com.hisobnoma.platform.mobile.repository.DeviceTokenRepository;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileAuthControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/mobile/auth";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private DeviceTokenRepository deviceTokenRepository;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("FullFlow MobAuth Tenant").code("FULLFLOW_MOBAUTH").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("mobauth_testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        entityManager.clear();
    }

    private RequestPostProcessor auth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(authentication);
    }

    // ---- POST /login ----

    @Test
    void login_validCredentials_returnsTokens() throws Exception {
        Map<String, Object> loginRequest = Map.of(
                "username", "mobauth_testuser",
                "password", "password123"
        );

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "TestMobileApp/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("mobauth_testuser"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        Map<String, Object> loginRequest = Map.of(
                "username", "mobauth_testuser",
                "password", "wrongpassword"
        );

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        Map<String, Object> loginRequest = Map.of(
                "username", "",
                "password", "password123"
        );

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /register-device ----

    @Test
    void registerDevice_validRequest_returnsDevice() throws Exception {
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("dev-1")
                .fcmToken("token123")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Test Phone")
                .build();

        mockMvc.perform(post(BASE_URL + "/register-device")
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value("dev-1"))
                .andExpect(jsonPath("$.data.fcmToken").value("token123"))
                .andExpect(jsonPath("$.data.platform").value("ANDROID"))
                .andExpect(jsonPath("$.data.deviceName").value("Test Phone"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void registerDevice_updateExisting_updatesToken() throws Exception {
        // First registration
        RegisterDeviceRequest request1 = RegisterDeviceRequest.builder()
                .deviceId("dev-update")
                .fcmToken("original-token")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Phone v1")
                .build();

        mockMvc.perform(post(BASE_URL + "/register-device")
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fcmToken").value("original-token"));

        // Second registration with same deviceId but updated token
        RegisterDeviceRequest request2 = RegisterDeviceRequest.builder()
                .deviceId("dev-update")
                .fcmToken("updated-token")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Phone v2")
                .build();

        mockMvc.perform(post(BASE_URL + "/register-device")
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fcmToken").value("updated-token"))
                .andExpect(jsonPath("$.data.deviceName").value("Phone v2"));

        // Verify only one device exists in the DB for this deviceId
        List<DeviceToken> devices = deviceTokenRepository
                .findByUserIdAndTenantIdAndActiveTrue(user.getId(), tenant.getId());
        long matchCount = devices.stream()
                .filter(d -> "dev-update".equals(d.getDeviceId()))
                .count();
        org.assertj.core.api.Assertions.assertThat(matchCount).isEqualTo(1);
    }

    // ---- GET /devices ----

    @Test
    void getDevices_withRegisteredDevice_returnsList() throws Exception {
        // Register a device first
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("dev-list")
                .fcmToken("list-token")
                .platform(DeviceToken.Platform.IOS)
                .deviceName("iPhone Test")
                .build();

        mockMvc.perform(post(BASE_URL + "/register-device")
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get devices
        mockMvc.perform(get(BASE_URL + "/devices")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].deviceId").value("dev-list"))
                .andExpect(jsonPath("$.data[0].platform").value("IOS"))
                .andExpect(jsonPath("$.data[0].deviceName").value("iPhone Test"));
    }

    @Test
    void getDevices_noDevices_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/devices")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---- DELETE /devices/{deviceId} ----

    @Test
    void deactivateDevice_existingDevice_deactivates() throws Exception {
        // Register a device first
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("dev-deactivate")
                .fcmToken("deactivate-token")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Deactivate Phone")
                .build();

        mockMvc.perform(post(BASE_URL + "/register-device")
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Deactivate the device
        mockMvc.perform(delete(BASE_URL + "/devices/{deviceId}", "dev-deactivate")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Device deactivated successfully"));

        // Verify device no longer appears in active device list
        mockMvc.perform(get(BASE_URL + "/devices")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---- POST /logout ----

    @Test
    void logout_withDeviceId_deactivatesDevice() throws Exception {
        // Register a device first
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("dev-logout")
                .fcmToken("logout-token")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Logout Phone")
                .build();

        mockMvc.perform(post(BASE_URL + "/register-device")
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Logout with deviceId
        mockMvc.perform(post(BASE_URL + "/logout")
                        .param("deviceId", "dev-logout")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Verify device is no longer active
        mockMvc.perform(get(BASE_URL + "/devices")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void logout_withoutDeviceId_succeeds() throws Exception {
        mockMvc.perform(post(BASE_URL + "/logout")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
