package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.auth.dto.AuthResponse;
import com.hisobnoma.platform.auth.dto.LoginRequest;
import com.hisobnoma.platform.auth.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.service.AuthService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.mobile.dto.DeviceTokenDTO;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.service.DeviceTokenService;
import com.hisobnoma.platform.mobile.service.MobileAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mobile authentication and device registration controller.
 */
@RestController
@RequestMapping("/api/v1/mobile/auth")
@RequiredArgsConstructor
@Tag(name = "Mobile Authentication", description = "Mobile authentication and device registration APIs")
public class MobileAuthController {

    private final AuthService authService;
    private final DeviceTokenService deviceTokenService;
    private final MobileAlertService alertService;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping("/login")
    @Operation(summary = "Mobile login", description = "Authenticate user for mobile app")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/register-device")
    @Operation(summary = "Register device", description = "Register mobile device for push notifications")
    public ResponseEntity<ApiResponse<DeviceTokenDTO>> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        DeviceTokenDTO response = deviceTokenService.registerDevice(request);
        // Initialize default alert preferences for the user
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();
        Long tenantId = securityContextHelper.getRequiredTenantId();
        alertService.initializeDefaultPreferences(userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/devices")
    @Operation(summary = "Get registered devices", description = "Get all registered devices for the current user")
    public ResponseEntity<ApiResponse<List<DeviceTokenDTO>>> getDevices() {
        List<DeviceTokenDTO> devices = deviceTokenService.getDevices();
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @DeleteMapping("/devices/{deviceId}")
    @Operation(summary = "Deactivate device", description = "Remove device from push notification list")
    public ResponseEntity<ApiResponse<Void>> deactivateDevice(@PathVariable String deviceId) {
        deviceTokenService.deactivateDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device deactivated successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Mobile logout", description = "Logout from mobile app and deactivate device")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestParam(required = false) String deviceId,
            @RequestHeader("Authorization") String token) {
        if (deviceId != null) {
            deviceTokenService.deactivateDevice(deviceId);
        }
        authService.logout(token.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
