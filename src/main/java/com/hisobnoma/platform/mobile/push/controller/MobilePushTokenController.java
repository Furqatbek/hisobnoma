package com.hisobnoma.platform.mobile.push.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.mobile.push.dto.RegisterPushTokenRequest;
import com.hisobnoma.platform.mobile.push.dto.RemovePushTokenRequest;
import com.hisobnoma.platform.mobile.push.service.DevicePushTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * APNs device-token registration for the authenticated mobile user (no special permission — a
 * user manages their own device's push token).
 */
@RestController
@RequestMapping("/api/v1/mobile/devices")
@RequiredArgsConstructor
public class MobilePushTokenController {

    private final DevicePushTokenService tokenService;

    @PostMapping("/push-token")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterPushTokenRequest request) {
        tokenService.registerToken(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Push token registered"));
    }

    @DeleteMapping("/push-token")
    public ResponseEntity<ApiResponse<Void>> remove(@Valid @RequestBody RemovePushTokenRequest request) {
        tokenService.removeToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Push token removed"));
    }
}
