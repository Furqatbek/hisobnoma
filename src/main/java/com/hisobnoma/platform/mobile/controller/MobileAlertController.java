package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.mobile.dto.AlertPreferenceDTO;
import com.hisobnoma.platform.mobile.dto.MobileAlertDTO;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.mobile.service.MobileAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Mobile alerts and notifications controller.
 */
@RestController
@RequestMapping("/api/v1/mobile/alerts")
@RequiredArgsConstructor
@Tag(name = "Mobile Alerts", description = "Mobile alerts and notification management APIs")
public class MobileAlertController {

    private final MobileAlertService alertService;

    @GetMapping
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Get all alerts", description = "Returns paginated list of all alerts for the user")
    public ResponseEntity<ApiResponse<PageResponse<MobileAlertDTO>>> getAlerts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MobileAlertDTO> alerts = alertService.getAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(alerts)));
    }

    @GetMapping("/unread")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Get unread alerts", description = "Returns paginated list of unread alerts")
    public ResponseEntity<ApiResponse<PageResponse<MobileAlertDTO>>> getUnreadAlerts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MobileAlertDTO> alerts = alertService.getUnreadAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(alerts)));
    }

    @GetMapping("/type/{alertType}")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Get alerts by type", description = "Returns alerts filtered by type")
    public ResponseEntity<ApiResponse<PageResponse<MobileAlertDTO>>> getAlertsByType(
            @Parameter(description = "Alert type") @PathVariable MobileAlert.AlertType alertType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MobileAlertDTO> alerts = alertService.getAlertsByType(alertType, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(alerts)));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Get unread count", description = "Returns count of unread alerts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = alertService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Mark as read", description = "Mark a specific alert as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert marked as read"));
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Mark all as read", description = "Mark all alerts as read for the user")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        alertService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(null, "All alerts marked as read"));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_VIEW')")
    @Operation(summary = "Get alert settings", description = "Returns alert notification preferences")
    public ResponseEntity<ApiResponse<List<AlertPreferenceDTO>>> getAlertSettings() {
        List<AlertPreferenceDTO> preferences = alertService.getAlertPreferences();
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PutMapping("/settings/{alertType}")
    @PreAuthorize("hasAuthority('MOBILE_ALERTS_MANAGE')")
    @Operation(summary = "Update alert settings", description = "Update alert notification preferences for a type")
    public ResponseEntity<ApiResponse<AlertPreferenceDTO>> updateAlertSettings(
            @Parameter(description = "Alert type") @PathVariable MobileAlert.AlertType alertType,
            @RequestBody AlertPreferenceDTO dto) {
        AlertPreferenceDTO updated = alertService.updateAlertPreference(alertType, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
