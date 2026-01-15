package com.hisobnoma.platform.reports.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.reports.dto.*;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportDefinition.ExportFormat;
import com.hisobnoma.platform.reports.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for reports management and generation.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Report management and generation APIs")
public class ReportController {

    private final ReportService reportService;
    private final InventoryReportService inventoryReportService;
    private final SalesReportService salesReportService;
    private final FinancialReportService financialReportService;
    private final ReportExportService exportService;

    // ==================== Report Definitions ====================

    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "List report definitions", description = "Get all available report definitions")
    public ResponseEntity<ApiResponse<PageResponse<ReportDefinitionDTO>>> listReportDefinitions(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ReportDefinitionDTO> reports = reportService.getReportDefinitions(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(reports)));
    }

    @GetMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get report definition", description = "Get a specific report definition by ID")
    public ResponseEntity<ApiResponse<ReportDefinitionDTO>> getReportDefinition(@PathVariable Long id) {
        ReportDefinitionDTO report = reportService.getReportDefinition(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/definitions/code/{code}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get report by code", description = "Get a specific report definition by code")
    public ResponseEntity<ApiResponse<ReportDefinitionDTO>> getReportByCode(@PathVariable String code) {
        ReportDefinitionDTO report = reportService.getReportDefinitionByCode(code);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/definitions/category/{category}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "List reports by category", description = "Get report definitions filtered by category")
    public ResponseEntity<ApiResponse<PageResponse<ReportDefinitionDTO>>> listByCategory(
            @PathVariable ReportDefinition.ReportCategory category,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ReportDefinitionDTO> reports = reportService.getReportsByCategory(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(reports)));
    }

    // ==================== Inventory Reports ====================

    @PostMapping("/inventory/stock-on-hand")
    @PreAuthorize("hasAuthority('REPORT_INVENTORY_VIEW')")
    @Operation(summary = "Generate Stock on Hand Report", description = "Generate stock on hand report with optional filters")
    public ResponseEntity<ApiResponse<StockOnHandReportDTO>> generateStockOnHandReport(
            @Valid @RequestBody GenerateReportRequest request) {
        StockOnHandReportDTO report = inventoryReportService.generateStockOnHandReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/inventory/stock-on-hand/export")
    @PreAuthorize("hasAuthority('REPORT_INVENTORY_VIEW')")
    @Operation(summary = "Export Stock on Hand Report", description = "Generate and export stock on hand report")
    public ResponseEntity<byte[]> exportStockOnHandReport(
            @Valid @RequestBody GenerateReportRequest request) throws IOException {
        StockOnHandReportDTO report = inventoryReportService.generateStockOnHandReport(request);
        byte[] data = exportService.exportReport(report, request.getExportFormat(), "STOCK_ON_HAND");
        return buildExportResponse(data, request.getExportFormat(), "stock-on-hand");
    }

    @PostMapping("/inventory/valuation")
    @PreAuthorize("hasAuthority('REPORT_INVENTORY_VIEW')")
    @Operation(summary = "Generate Inventory Valuation Report", description = "Generate inventory valuation report")
    public ResponseEntity<ApiResponse<InventoryValuationReportDTO>> generateInventoryValuationReport(
            @Valid @RequestBody GenerateReportRequest request) {
        InventoryValuationReportDTO report = inventoryReportService.generateInventoryValuationReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/inventory/valuation/export")
    @PreAuthorize("hasAuthority('REPORT_INVENTORY_VIEW')")
    @Operation(summary = "Export Inventory Valuation Report", description = "Generate and export inventory valuation report")
    public ResponseEntity<byte[]> exportInventoryValuationReport(
            @Valid @RequestBody GenerateReportRequest request) throws IOException {
        InventoryValuationReportDTO report = inventoryReportService.generateInventoryValuationReport(request);
        byte[] data = exportService.exportReport(report, request.getExportFormat(), "INVENTORY_VALUATION");
        return buildExportResponse(data, request.getExportFormat(), "inventory-valuation");
    }

    // ==================== Sales Reports ====================

    @PostMapping("/sales/summary")
    @PreAuthorize("hasAuthority('REPORT_SALES_VIEW')")
    @Operation(summary = "Generate Sales Summary Report", description = "Generate sales summary report for a date range")
    public ResponseEntity<ApiResponse<SalesSummaryReportDTO>> generateSalesSummaryReport(
            @Valid @RequestBody GenerateReportRequest request) {
        SalesSummaryReportDTO report = salesReportService.generateSalesSummaryReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/sales/summary/export")
    @PreAuthorize("hasAuthority('REPORT_SALES_VIEW')")
    @Operation(summary = "Export Sales Summary Report", description = "Generate and export sales summary report")
    public ResponseEntity<byte[]> exportSalesSummaryReport(
            @Valid @RequestBody GenerateReportRequest request) throws IOException {
        SalesSummaryReportDTO report = salesReportService.generateSalesSummaryReport(request);
        byte[] data = exportService.exportReport(report, request.getExportFormat(), "SALES_SUMMARY");
        return buildExportResponse(data, request.getExportFormat(), "sales-summary");
    }

    // ==================== Financial Reports ====================

    @PostMapping("/financial/trial-balance")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL_VIEW')")
    @Operation(summary = "Generate Trial Balance Report", description = "Generate trial balance report")
    public ResponseEntity<ApiResponse<TrialBalanceReportDTO>> generateTrialBalanceReport(
            @Valid @RequestBody GenerateReportRequest request) {
        TrialBalanceReportDTO report = financialReportService.generateTrialBalanceReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/financial/trial-balance/export")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL_VIEW')")
    @Operation(summary = "Export Trial Balance Report", description = "Generate and export trial balance report")
    public ResponseEntity<byte[]> exportTrialBalanceReport(
            @Valid @RequestBody GenerateReportRequest request) throws IOException {
        TrialBalanceReportDTO report = financialReportService.generateTrialBalanceReport(request);
        byte[] data = exportService.exportReport(report, request.getExportFormat(), "TRIAL_BALANCE");
        return buildExportResponse(data, request.getExportFormat(), "trial-balance");
    }

    @PostMapping("/financial/ar-aging")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL_VIEW')")
    @Operation(summary = "Generate AR Aging Report", description = "Generate accounts receivable aging report")
    public ResponseEntity<ApiResponse<AgingReportDTO>> generateARAgingReport(
            @Valid @RequestBody GenerateReportRequest request) {
        AgingReportDTO report = financialReportService.generateARAgingReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/financial/ar-aging/export")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL_VIEW')")
    @Operation(summary = "Export AR Aging Report", description = "Generate and export AR aging report")
    public ResponseEntity<byte[]> exportARAgingReport(
            @Valid @RequestBody GenerateReportRequest request) throws IOException {
        AgingReportDTO report = financialReportService.generateARAgingReport(request);
        byte[] data = exportService.exportReport(report, request.getExportFormat(), "AR_AGING");
        return buildExportResponse(data, request.getExportFormat(), "ar-aging");
    }

    @PostMapping("/financial/ap-aging")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL_VIEW')")
    @Operation(summary = "Generate AP Aging Report", description = "Generate accounts payable aging report")
    public ResponseEntity<ApiResponse<AgingReportDTO>> generateAPAgingReport(
            @Valid @RequestBody GenerateReportRequest request) {
        AgingReportDTO report = financialReportService.generateAPAgingReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/financial/ap-aging/export")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL_VIEW')")
    @Operation(summary = "Export AP Aging Report", description = "Generate and export AP aging report")
    public ResponseEntity<byte[]> exportAPAgingReport(
            @Valid @RequestBody GenerateReportRequest request) throws IOException {
        AgingReportDTO report = financialReportService.generateAPAgingReport(request);
        byte[] data = exportService.exportReport(report, request.getExportFormat(), "AP_AGING");
        return buildExportResponse(data, request.getExportFormat(), "ap-aging");
    }

    // ==================== Report Schedules ====================

    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('REPORT_SCHEDULE_VIEW')")
    @Operation(summary = "List report schedules", description = "Get all report schedules")
    public ResponseEntity<ApiResponse<PageResponse<ReportScheduleDTO>>> listSchedules(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportScheduleDTO> schedules = reportService.getSchedules(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(schedules)));
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('REPORT_SCHEDULE_VIEW')")
    @Operation(summary = "Get schedule", description = "Get a specific report schedule")
    public ResponseEntity<ApiResponse<ReportScheduleDTO>> getSchedule(@PathVariable Long id) {
        ReportScheduleDTO schedule = reportService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAuthority('REPORT_SCHEDULE_MANAGE')")
    @Operation(summary = "Create schedule", description = "Create a new report schedule")
    public ResponseEntity<ApiResponse<ReportScheduleDTO>> createSchedule(
            @Valid @RequestBody ReportScheduleDTO dto) {
        ReportScheduleDTO created = reportService.createSchedule(dto);
        return ResponseEntity.ok(ApiResponse.success(created, "Report schedule created successfully"));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('REPORT_SCHEDULE_MANAGE')")
    @Operation(summary = "Update schedule", description = "Update an existing report schedule")
    public ResponseEntity<ApiResponse<ReportScheduleDTO>> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ReportScheduleDTO dto) {
        ReportScheduleDTO updated = reportService.updateSchedule(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Report schedule updated successfully"));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('REPORT_SCHEDULE_MANAGE')")
    @Operation(summary = "Delete schedule", description = "Delete a report schedule")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long id) {
        reportService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Report schedule deleted successfully"));
    }

    @PutMapping("/schedules/{id}/toggle")
    @PreAuthorize("hasAuthority('REPORT_SCHEDULE_MANAGE')")
    @Operation(summary = "Toggle schedule", description = "Enable or disable a report schedule")
    public ResponseEntity<ApiResponse<ReportScheduleDTO>> toggleSchedule(
            @PathVariable Long id,
            @RequestParam boolean active) {
        ReportScheduleDTO updated = reportService.toggleSchedule(id, active);
        return ResponseEntity.ok(ApiResponse.success(updated,
                active ? "Schedule enabled" : "Schedule disabled"));
    }

    // ==================== Report Executions ====================

    @GetMapping("/executions")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "List report executions", description = "Get all report execution history")
    public ResponseEntity<ApiResponse<PageResponse<ReportExecutionDTO>>> listExecutions(
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportExecutionDTO> executions = reportService.getExecutions(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(executions)));
    }

    @GetMapping("/executions/{id}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get execution details", description = "Get details of a specific report execution")
    public ResponseEntity<ApiResponse<ReportExecutionDTO>> getExecution(@PathVariable Long id) {
        ReportExecutionDTO execution = reportService.getExecution(id);
        return ResponseEntity.ok(ApiResponse.success(execution));
    }

    @GetMapping("/executions/report/{reportId}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get executions by report", description = "Get execution history for a specific report")
    public ResponseEntity<ApiResponse<PageResponse<ReportExecutionDTO>>> getExecutionsByReport(
            @PathVariable Long reportId,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportExecutionDTO> executions = reportService.getExecutionsByReport(reportId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(executions)));
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<byte[]> buildExportResponse(byte[] data, ExportFormat format, String reportName) {
        String filename = reportName + "-" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String contentType;
        String extension;

        switch (format) {
            case EXCEL -> {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = ".xlsx";
            }
            case CSV -> {
                contentType = "text/csv";
                extension = ".csv";
            }
            case PDF -> {
                contentType = "application/pdf";
                extension = ".pdf";
            }
            default -> {
                contentType = "application/json";
                extension = ".json";
            }
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + extension + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
