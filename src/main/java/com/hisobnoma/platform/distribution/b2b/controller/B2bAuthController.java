package com.hisobnoma.platform.distribution.b2b.controller;

import com.hisobnoma.platform.distribution.b2b.dto.B2bAuthResponse;
import com.hisobnoma.platform.distribution.b2b.dto.B2bLoginRequest;
import com.hisobnoma.platform.distribution.b2b.dto.B2bProfileDto;
import com.hisobnoma.platform.distribution.b2b.service.B2bAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public B2B marketplace authentication. Endpoints are anonymous at the Spring Security
 * layer (whitelisted under {@code /api/v1/b2b/**}); the tenant comes from the
 * {@code X-Tenant-ID} header and the buyer identity from the returned bearer token.
 */
@RestController
@RequestMapping("/api/v1/b2b")
@RequiredArgsConstructor
public class B2bAuthController {

    private final B2bAuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<B2bAuthResponse> login(@Valid @RequestBody B2bLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<B2bProfileDto> me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok(authService.getProfile(authorization));
    }
}
