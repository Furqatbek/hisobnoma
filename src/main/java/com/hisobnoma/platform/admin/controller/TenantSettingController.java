package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/settings/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant Settings", description = "Tenant-specific configuration settings APIs")
public class TenantSettingController {

    private final TenantSettingService tenantSettingService;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_VIEW')")
    @Operation(summary = "Get all tenant settings", description = "Returns all active tenant settings")
    public ResponseEntity<ApiResponse<List<TenantSettingDTO>>> getAllSettings() {
        List<TenantSettingDTO> settings = tenantSettingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_VIEW')")
    @Operation(summary = "Get setting categories", description = "Returns all distinct setting categories for the tenant")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = tenantSettingService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_VIEW')")
    @Operation(summary = "Get settings by category", description = "Returns all tenant settings for a specific category")
    public ResponseEntity<ApiResponse<List<TenantSettingDTO>>> getSettingsByCategory(
            @Parameter(description = "Setting category") @PathVariable String category) {
        List<TenantSettingDTO> settings = tenantSettingService.getSettingsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_MANAGE')")
    @Operation(summary = "Create tenant setting", description = "Creates a new tenant-specific setting")
    public ResponseEntity<ApiResponse<TenantSettingDTO>> createSetting(@RequestBody TenantSettingDTO dto) {
        TenantSettingDTO created = tenantSettingService.createSetting(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_MANAGE')")
    @Operation(summary = "Update tenant setting", description = "Updates an existing tenant setting")
    public ResponseEntity<ApiResponse<TenantSettingDTO>> updateSetting(
            @Parameter(description = "Setting key") @PathVariable String key,
            @RequestBody TenantSettingDTO dto) {
        TenantSettingDTO updated = tenantSettingService.updateSetting(key, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{key}/value")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_MANAGE')")
    @Operation(summary = "Update setting value", description = "Updates only the value of a tenant setting")
    public ResponseEntity<ApiResponse<TenantSettingDTO>> updateSettingValue(
            @Parameter(description = "Setting key") @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        TenantSettingDTO updated = tenantSettingService.updateSettingValue(key, value);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_MANAGE')")
    @Operation(summary = "Batch update settings", description = "Updates multiple tenant settings at once")
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody Map<String, String> settings) {
        tenantSettingService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.success(null, "Settings updated successfully"));
    }

    @GetMapping("/map")
    @PreAuthorize("hasAuthority('TENANT_SETTINGS_VIEW')")
    @Operation(summary = "Get settings as map", description = "Returns all tenant settings as a key-value map")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSettingsAsMap() {
        Map<String, String> settings = tenantSettingService.getSettingsAsMap();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
}
