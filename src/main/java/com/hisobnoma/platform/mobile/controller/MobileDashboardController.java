package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.mobile.dto.FinancialSummaryDTO;
import com.hisobnoma.platform.mobile.dto.InventorySummaryDTO;
import com.hisobnoma.platform.mobile.dto.RevenueSummaryDTO;
import com.hisobnoma.platform.mobile.dto.SalesSummaryDTO;
import com.hisobnoma.platform.mobile.service.MobileDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Mobile dashboard controller for revenue, sales, inventory, and financial summaries.
 */
@RestController
@RequestMapping("/api/v1/mobile/dashboard")
@RequiredArgsConstructor
@Tag(name = "Mobile Dashboard", description = "Mobile dashboard APIs for business overview")
public class MobileDashboardController {

    private final MobileDashboardService dashboardService;

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyAuthority('MOBILE_DASHBOARD_VIEW', 'REPORTS_SALES_VIEW', 'ADMIN_DASHBOARD_VIEW')")
    @Operation(summary = "Get revenue summary", description = "Returns revenue summary with comparisons")
    public ResponseEntity<ApiResponse<RevenueSummaryDTO>> getRevenueSummary() {
        RevenueSummaryDTO summary = dashboardService.getRevenueSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/revenue/chart")
    @PreAuthorize("hasAnyAuthority('MOBILE_DASHBOARD_VIEW', 'REPORTS_SALES_VIEW', 'ADMIN_DASHBOARD_VIEW')")
    @Operation(summary = "Get revenue chart data", description = "Returns revenue data for charts")
    public ResponseEntity<ApiResponse<RevenueSummaryDTO>> getRevenueChartData(
            @Parameter(description = "Period: hourly, daily, monthly") @RequestParam(defaultValue = "daily") String period) {
        RevenueSummaryDTO chartData = dashboardService.getRevenueChartData(period);
        return ResponseEntity.ok(ApiResponse.success(chartData));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyAuthority('MOBILE_DASHBOARD_VIEW', 'INVENTORY_STOCK_READ', 'ADMIN_DASHBOARD_VIEW')")
    @Operation(summary = "Get inventory summary", description = "Returns inventory overview with alerts")
    public ResponseEntity<ApiResponse<InventorySummaryDTO>> getInventorySummary() {
        InventorySummaryDTO summary = dashboardService.getInventorySummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAnyAuthority('MOBILE_DASHBOARD_VIEW', 'FINANCE_REPORTS_VIEW', 'ADMIN_DASHBOARD_VIEW')")
    @Operation(summary = "Get financial summary", description = "Returns financial overview including AR/AP")
    public ResponseEntity<ApiResponse<FinancialSummaryDTO>> getFinancialSummary() {
        FinancialSummaryDTO summary = dashboardService.getFinancialSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
