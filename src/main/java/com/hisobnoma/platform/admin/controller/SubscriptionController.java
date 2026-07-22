package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.SubscriptionDto;
import com.hisobnoma.platform.admin.service.SubscriptionService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "Self-service tenant plan management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_VIEW')")
    @Operation(summary = "Current plan, usage and switchable plans")
    public ResponseEntity<ApiResponse<SubscriptionDto>> getSubscription() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getSubscription()));
    }

    @PostMapping("/plan")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_MANAGE')")
    @Operation(summary = "Upgrade or downgrade the tenant's plan")
    public ResponseEntity<ApiResponse<SubscriptionDto>> changePlan(
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.changePlan(request.get("plan")), "Plan changed"));
    }
}
