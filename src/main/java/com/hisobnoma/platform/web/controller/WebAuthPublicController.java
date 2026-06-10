package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.dto.RequestOtpRequest;
import com.hisobnoma.platform.web.dto.VerifyOtpRequest;
import com.hisobnoma.platform.web.dto.WebAuthResponse;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.service.WebAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Phone + SMS OTP auth and the customer's own data for the mobile app.
 * Endpoints are anonymous at the security-chain level (/api/v1/web/**);
 * /me endpoints validate the web-customer bearer token themselves.
 */
@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class WebAuthPublicController {

    private final WebAuthService authService;

    @PostMapping("/auth/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @Valid @RequestBody RequestOtpRequest request,
            HttpServletRequest httpRequest) {
        authService.requestOtp(request.getPhone(), clientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(null, "Code sent"));
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<ApiResponse<WebAuthResponse>> verify(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.verifyOtp(request.getPhone(), request.getCode(), request.getName())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> me(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "phone", customer.getPhone(),
                "name", customer.getName() != null ? customer.getName() : "")));
    }

    @GetMapping("/me/orders")
    public ResponseEntity<PageResponse<PublicOrderDto>> myOrders(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PublicOrderDto> page = authService.getMyOrders(authorization, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
