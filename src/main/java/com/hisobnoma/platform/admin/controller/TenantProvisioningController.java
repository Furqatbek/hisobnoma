package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.ProvisionTenantRequest;
import com.hisobnoma.platform.admin.dto.ProvisionTenantResult;
import com.hisobnoma.platform.admin.service.TenantProvisioningService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-operator endpoint: onboard a new tenant in one call (tenant + default chart of
 * accounts + first ADMIN user + open fiscal year). Guarded by TENANT_PROVISION, seeded to
 * SUPER_ADMIN only.
 */
@RestController
@RequestMapping("/api/v1/admin/tenants")
@RequiredArgsConstructor
public class TenantProvisioningController {

    private final TenantProvisioningService provisioningService;

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_PROVISION')")
    public ResponseEntity<ApiResponse<ProvisionTenantResult>> provision(
            @Valid @RequestBody ProvisionTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(provisioningService.provision(request), "Tenant provisioned"));
    }
}
