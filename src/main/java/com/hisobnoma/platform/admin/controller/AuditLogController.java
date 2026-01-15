package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.AuditLogDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.service.AuditLogService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit trail and activity logging APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get audit logs", description = "Returns paginated list of audit logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get audit logs by user", description = "Returns audit logs for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogsByUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getAuditLogsByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get audit logs by entity", description = "Returns audit logs for a specific entity")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogsByEntity(
            @Parameter(description = "Entity type") @PathVariable String entityType,
            @Parameter(description = "Entity ID") @PathVariable Long entityId,
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getAuditLogsByEntity(entityType, entityId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get audit logs by action", description = "Returns audit logs for a specific action type")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogsByAction(
            @Parameter(description = "Action type") @PathVariable AuditLog.AuditAction action,
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getAuditLogsByAction(action, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/module/{module}")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get audit logs by module", description = "Returns audit logs for a specific module")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogsByModule(
            @Parameter(description = "Module name") @PathVariable String module,
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getAuditLogsByModule(module, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get audit logs by date range", description = "Returns audit logs within a date range")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogsByDateRange(
            @Parameter(description = "Start date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getAuditLogsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/failed")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get failed actions", description = "Returns audit logs for failed actions")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getFailedActions(
            @PageableDefault(size = 20, sort = "actionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogDTO> logs = auditLogService.getFailedActions(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/stats/actions")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get action statistics", description = "Returns action count statistics")
    public ResponseEntity<ApiResponse<List<Object[]>>> getActionStats(
            @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "7") int days) {
        List<Object[]> stats = auditLogService.getActionStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/stats/modules")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get module activity statistics", description = "Returns activity count by module")
    public ResponseEntity<ApiResponse<List<Object[]>>> getModuleStats(
            @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "7") int days) {
        List<Object[]> stats = auditLogService.getModuleActivityStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/stats/users")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get most active users", description = "Returns most active users by audit log count")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMostActiveUsers(
            @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "7") int days) {
        List<Object[]> stats = auditLogService.getMostActiveUsers(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/stats/failed-logins")
    @PreAuthorize("hasAuthority('ADMIN_AUDIT_VIEW')")
    @Operation(summary = "Get failed login count", description = "Returns count of failed login attempts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFailedLoginCount(
            @Parameter(description = "Number of hours to look back") @RequestParam(defaultValue = "24") int hours) {
        long count = auditLogService.getFailedLoginCount(hours);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }
}
