package com.hisobnoma.platform.auth.controller;

import com.hisobnoma.platform.auth.dto.*;
import com.hisobnoma.platform.auth.service.AuthService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username/phone and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/pin-login")
    @Operation(summary = "Login with PIN code (POS)")
    public ResponseEntity<ApiResponse<AuthResponse>> pinLogin(
            @Valid @RequestBody PinLoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.loginWithPin(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @GetMapping("/users/list")
    @Operation(summary = "Get active users for POS login screen")
    public ResponseEntity<ApiResponse<List<UserListItem>>> getActiveUsers() {
        return ResponseEntity.ok(ApiResponse.success(authService.getActiveUserList()));
    }

    @PutMapping("/set-pin")
    @Operation(summary = "Set PIN code for current user")
    public ResponseEntity<ApiResponse<Void>> setPin(@RequestBody Map<String, String> request) {
        String pin = request.get("pin");
        if (pin == null || pin.length() < 4 || pin.length() > 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PIN must be 4-6 digits"));
        }
        authService.setPin(pin);
        return ResponseEntity.ok(ApiResponse.success("PIN set successfully"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate tokens")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser() {
        AuthResponse.UserInfo userInfo = authService.getCurrentUserInfo();
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String token = authService.forgotPassword(request);
        // In production, don't return the token - send via SMS/notification
        return ResponseEntity.ok(ApiResponse.success(token, "If the account exists, a reset link will be sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
