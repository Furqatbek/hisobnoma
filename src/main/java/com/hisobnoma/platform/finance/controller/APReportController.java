package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.finance.dto.APAgingReportDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto.VendorBalanceDto;
import com.hisobnoma.platform.finance.service.APReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ap/reports")
@RequiredArgsConstructor
public class APReportController {

    private final APReportService apReportService;

    /**
     * Get AP Aging Report.
     * Shows all outstanding payables grouped by aging buckets (current, 1-30, 31-60, 61-90, >90 days).
     */
    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<APAgingReportDto>> getAgingReport() {
        APAgingReportDto report = apReportService.generateAgingReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Get Vendor Balance Report.
     * Shows balances for all vendors with outstanding payables.
     */
    @GetMapping("/vendor-balance")
    @PreAuthorize("hasAuthority('FINANCE_VENDOR_STATEMENT')")
    public ResponseEntity<ApiResponse<VendorBalanceReportDto>> getVendorBalanceReport() {
        VendorBalanceReportDto report = apReportService.generateVendorBalanceReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Get Vendor Statement.
     * Shows detailed balance information for a specific vendor.
     */
    @GetMapping("/vendor/{vendorId}/statement")
    @PreAuthorize("hasAuthority('FINANCE_VENDOR_STATEMENT')")
    public ResponseEntity<ApiResponse<VendorBalanceDto>> getVendorStatement(@PathVariable Long vendorId) {
        VendorBalanceDto statement = apReportService.getVendorStatement(vendorId);
        return ResponseEntity.ok(ApiResponse.success(statement));
    }
}
