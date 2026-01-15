package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.service.SystemSettingService;
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
@RequestMapping("/api/v1/admin/settings/system")
@RequiredArgsConstructor
@Tag(name = "System Settings", description = "System-wide configuration settings APIs")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_VIEW')")
    @Operation(summary = "Get all system settings", description = "Returns all active system settings")
    public ResponseEntity<ApiResponse<List<SystemSettingDTO>>> getAllSettings() {
        List<SystemSettingDTO> settings = systemSettingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_VIEW')")
    @Operation(summary = "Get setting categories", description = "Returns all distinct setting categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = systemSettingService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_VIEW')")
    @Operation(summary = "Get settings by category", description = "Returns all settings for a specific category")
    public ResponseEntity<ApiResponse<List<SystemSettingDTO>>> getSettingsByCategory(
            @Parameter(description = "Setting category") @PathVariable String category) {
        List<SystemSettingDTO> settings = systemSettingService.getSettingsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_VIEW')")
    @Operation(summary = "Get setting by key", description = "Returns a specific setting by its key")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> getSetting(
            @Parameter(description = "Setting key") @PathVariable String key) {
        SystemSettingDTO setting = systemSettingService.getSetting(key);
        return ResponseEntity.ok(ApiResponse.success(setting));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    @Operation(summary = "Create system setting", description = "Creates a new system setting")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> createSetting(@RequestBody SystemSettingDTO dto) {
        SystemSettingDTO created = systemSettingService.createSetting(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    @Operation(summary = "Update system setting", description = "Updates an existing system setting")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> updateSetting(
            @Parameter(description = "Setting key") @PathVariable String key,
            @RequestBody SystemSettingDTO dto) {
        SystemSettingDTO updated = systemSettingService.updateSetting(key, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{key}/value")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    @Operation(summary = "Update setting value", description = "Updates only the value of a setting")
    public ResponseEntity<ApiResponse<SystemSettingDTO>> updateSettingValue(
            @Parameter(description = "Setting key") @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        SystemSettingDTO updated = systemSettingService.updateSettingValue(key, value);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    @Operation(summary = "Batch update settings", description = "Updates multiple settings at once")
    public ResponseEntity<ApiResponse<Void>> updateSettings(@RequestBody Map<String, String> settings) {
        systemSettingService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.success(null, "Settings updated successfully"));
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    @Operation(summary = "Delete system setting", description = "Deactivates a system setting")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(
            @Parameter(description = "Setting key") @PathVariable String key) {
        systemSettingService.deleteSetting(key);
        return ResponseEntity.ok(ApiResponse.success(null, "Setting deleted successfully"));
    }
}
