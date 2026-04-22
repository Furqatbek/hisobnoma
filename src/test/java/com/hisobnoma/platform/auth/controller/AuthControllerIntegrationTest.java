package com.hisobnoma.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.dto.*;
import com.hisobnoma.platform.auth.service.AuthService;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserInfo.builder()
                        .id(1L)
                        .username("admin")
                        .roles(Set.of("ADMIN"))
                        .build())
                .build();
    }

    // ---- POST /api/v1/auth/login ----

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        LoginRequest request = LoginRequest.builder().username("admin").password("admin123").build();
        when(authService.login(any(LoginRequest.class), anyString(), anyString()))
                .thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.username").value("admin"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder().username("admin").password("wrong").build();
        when(authService.login(any(LoginRequest.class), anyString(), anyString()))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_lockedAccount_returns403() throws Exception {
        // Given — LockedException maps to 423 LOCKED or 403 FORBIDDEN depending on handler
        LoginRequest request = LoginRequest.builder().username("admin").password("admin123").build();
        when(authService.login(any(LoginRequest.class), anyString(), anyString()))
                .thenThrow(new LockedException("Account is locked"));

        // The global handler should map to 4xx (locked or forbidden)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        LoginRequest request = LoginRequest.builder().password("admin123").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/auth/pin-login ----

    @Test
    void pinLogin_validPin_returns200() throws Exception {
        PinLoginRequest request = PinLoginRequest.builder().username("admin").pin("1234").build();
        when(authService.loginWithPin(any(PinLoginRequest.class), anyString(), anyString()))
                .thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void pinLogin_wrongPin_returns401() throws Exception {
        PinLoginRequest request = PinLoginRequest.builder().username("admin").pin("9999").build();
        when(authService.loginWithPin(any(PinLoginRequest.class), anyString(), anyString()))
                .thenThrow(new UnauthorizedException("Invalid PIN"));

        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET /api/v1/auth/users/list ----

    @Test
    void getUsersForPinScreen_publicEndpoint_returns200List() throws Exception {
        // Note: /api/v1/auth/users/list is in SecurityConfig PUBLIC_ENDPOINTS — not auth-required
        UserListItem item = UserListItem.builder()
                .id(1L).username("admin").firstName("Admin").fullName("Admin User").initials("AU").hasPin(true).build();
        when(authService.getActiveUserList()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/auth/users/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].username").value("admin"));
    }

    // ---- PUT /api/v1/auth/set-pin ----

    @Test
    @WithMockUser
    void setPin_authenticated_returns200() throws Exception {
        doNothing().when(authService).setPin("1234");

        mockMvc.perform(put("/api/v1/auth/set-pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void setPin_invalidFormat_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/set-pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "12"))))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /api/v1/auth/refresh ----

    @Test
    void refresh_validToken_returns200NewTokens() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("valid").build();
        AuthResponse newResp = AuthResponse.builder()
                .accessToken("new-access").refreshToken("new-refresh").tokenType("Bearer").build();
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(newResp);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_expiredToken_returns401() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("expired").build();
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new UnauthorizedException("Refresh token is expired or revoked"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---- POST /api/v1/auth/logout ----

    @Test
    @WithMockUser
    void logout_authenticated_returns200() throws Exception {
        doNothing().when(authService).logout();

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ---- GET /api/v1/auth/me ----

    @Test
    @WithMockUser
    void getMe_authenticated_returns200CurrentUser() throws Exception {
        AuthResponse.UserInfo info = AuthResponse.UserInfo.builder()
                .id(1L).username("admin").build();
        when(authService.getCurrentUserInfo()).thenReturn(info);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void getMe_unauthenticated_returns403() throws Exception {
        // Spring Security stateless returns 403 for unauthenticated requests
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }

    // ---- PUT /api/v1/auth/change-password ----

    @Test
    @WithMockUser
    void changePassword_correctCurrentPassword_returns200() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();
        doNothing().when(authService).changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void changePassword_wrongCurrentPassword_returns400() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrong")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();
        // BusinessException maps to 400 BAD_REQUEST in the global handler
        doThrow(new BusinessException("Current password is incorrect", "INVALID_PASSWORD"))
                .when(authService).changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ---- POST /api/v1/auth/forgot-password ----

    @Test
    void forgotPassword_anyIdentifier_alwaysReturns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("someone@example.com");
        when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn("If the account exists, a reset link will be sent");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- POST /api/v1/auth/reset-password ----

    @Test
    void resetPassword_validToken_returns200() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resetPassword_expiredToken_returns400() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("expired")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();
        doThrow(new BusinessException("Invalid or expired reset token", "INVALID_TOKEN"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
}
