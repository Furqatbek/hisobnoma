package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.DashboardStatsDTO;
import com.hisobnoma.platform.admin.service.AdminDashboardService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin dashboard and business overview APIs")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD_VIEW')")
    @Operation(summary = "Get dashboard statistics", description = "Returns comprehensive business statistics and metrics")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        DashboardStatsDTO stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
