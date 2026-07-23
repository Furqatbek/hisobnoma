package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.util.ClientIpResolver;
import com.hisobnoma.platform.distribution.dto.AgentAuthResponse;
import com.hisobnoma.platform.distribution.dto.AgentProfileDto;
import com.hisobnoma.platform.distribution.dto.AgentRequestOtpRequest;
import com.hisobnoma.platform.distribution.dto.AgentVerifyOtpRequest;
import com.hisobnoma.platform.distribution.service.AgentAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public agent-app authentication (phone + SMS OTP). Endpoints are anonymous at
 * the Spring Security layer (whitelisted under {@code /api/v1/agent/**}); the
 * tenant comes from the {@code X-Tenant-ID} header on login, and the agent
 * identity from the returned bearer token thereafter.
 */
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentAuthController {

    private final AgentAuthService authService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/auth/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @Valid @RequestBody AgentRequestOtpRequest request,
            HttpServletRequest httpRequest) {
        authService.requestOtp(request.getPhone(), clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(null, "Code sent if the phone is registered"));
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<ApiResponse<AgentAuthResponse>> verify(
            @Valid @RequestBody AgentVerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.verifyOtp(request.getPhone(), request.getCode())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AgentProfileDto>> me(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(authorization)));
    }
}
