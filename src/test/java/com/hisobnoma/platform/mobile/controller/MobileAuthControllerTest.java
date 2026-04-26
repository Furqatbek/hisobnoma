package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.AuthResponse;
import com.hisobnoma.platform.auth.dto.LoginRequest;
import com.hisobnoma.platform.auth.dto.RefreshTokenRequest;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.auth.service.AuthService;
import com.hisobnoma.platform.mobile.dto.DeviceTokenDTO;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.service.DeviceTokenService;
import com.hisobnoma.platform.mobile.service.MobileAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private DeviceTokenService deviceTokenService;

    @MockBean
    private MobileAlertService alertService;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @Test
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("admin").password("admin123").build();
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token").refreshToken("refresh-token")
                .tokenType("Bearer").expiresIn(3600)
                .user(AuthResponse.UserInfo.builder().id(1L).username("admin").roles(Set.of("ADMIN")).build())
                .build();
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void refreshToken_valid_returns200() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("valid-token").build();
        AuthResponse response = AuthResponse.builder()
                .accessToken("new-token").refreshToken("new-refresh").tokenType("Bearer").expiresIn(3600).build();
        when(authService.refreshToken(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-token"));
    }

    @Test
    @WithMockUser
    void registerDevice_authenticated_returns200() throws Exception {
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("device-123")
                .fcmToken("fcm-token-abc")
                .platform(com.hisobnoma.platform.mobile.entity.DeviceToken.Platform.ANDROID)
                .build();
        DeviceTokenDTO dto = DeviceTokenDTO.builder().deviceId("device-123").build();
        when(deviceTokenService.registerDevice(any())).thenReturn(dto);

        UserPrincipal mockPrincipal = new UserPrincipal(1L, "testuser", "password", 1L, true, true, List.of());
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(mockPrincipal);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(1L);
        doNothing().when(alertService).initializeDefaultPreferences(any(), any());

        mockMvc.perform(post("/api/v1/mobile/auth/register-device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getDevices_returns200() throws Exception {
        DeviceTokenDTO dto = DeviceTokenDTO.builder().deviceId("device-123").build();
        when(deviceTokenService.getDevices()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/mobile/auth/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceId").value("device-123"));
    }

    @Test
    @WithMockUser
    void deactivateDevice_returns200() throws Exception {
        doNothing().when(deviceTokenService).deactivateDevice("device-123");

        mockMvc.perform(delete("/api/v1/mobile/auth/devices/device-123"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void logout_returns200() throws Exception {
        doNothing().when(authService).logout();

        mockMvc.perform(post("/api/v1/mobile/auth/logout"))
                .andExpect(status().isOk());
    }
}
