package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.finance.dto.ARAgingReportDto;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto;
import com.hisobnoma.platform.finance.service.ARReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finance/ar-reports")
@RequiredArgsConstructor
public class ARReportController {

    private final ARReportService arReportService;

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<ARAgingReportDto> getAgingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        return ResponseEntity.ok(arReportService.generateAgingReport(asOfDate));
    }

    @GetMapping("/customer-balance")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<CustomerBalanceReportDto> getCustomerBalanceReport() {
        return ResponseEntity.ok(arReportService.generateCustomerBalanceReport());
    }

    @GetMapping("/customer-balance/{customerId}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<CustomerBalanceReportDto.CustomerBalanceDto> getCustomerBalance(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(arReportService.getCustomerBalance(customerId));
    }
}
