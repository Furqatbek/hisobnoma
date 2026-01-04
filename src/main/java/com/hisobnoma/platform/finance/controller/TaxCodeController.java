package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.*;
import com.hisobnoma.platform.finance.entity.TaxType;
import com.hisobnoma.platform.finance.service.TaxCalculationService;
import com.hisobnoma.platform.finance.service.TaxCodeService;
import com.hisobnoma.platform.finance.service.TaxReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/tax-codes")
@RequiredArgsConstructor
public class TaxCodeController {

    private final TaxCodeService taxCodeService;
    private final TaxCalculationService taxCalculationService;
    private final TaxReportService taxReportService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<PageResponse<TaxCodeDto>> getTaxCodes(
            @PageableDefault(sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(taxCodeService.getTaxCodes(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<List<TaxCodeDto>> getAllTaxCodes() {
        return ResponseEntity.ok(taxCodeService.getAllTaxCodes());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<List<TaxCodeDto>> getActiveTaxCodes() {
        return ResponseEntity.ok(taxCodeService.getActiveTaxCodes());
    }

    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<List<TaxCodeDto>> getTaxCodesByType(@PathVariable TaxType type) {
        return ResponseEntity.ok(taxCodeService.getTaxCodesByType(type));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<List<TaxCodeDto>> getSalesTaxCodes() {
        return ResponseEntity.ok(taxCodeService.getSalesTaxCodes());
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<List<TaxCodeDto>> getPurchaseTaxCodes() {
        return ResponseEntity.ok(taxCodeService.getPurchaseTaxCodes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<TaxCodeDto> getTaxCode(@PathVariable Long id) {
        return ResponseEntity.ok(taxCodeService.getTaxCode(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<TaxCodeDto> getTaxCodeByCode(@PathVariable String code) {
        return ResponseEntity.ok(taxCodeService.getTaxCodeByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<PageResponse<TaxCodeDto>> searchTaxCodes(
            @RequestParam String query,
            @PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(taxCodeService.searchTaxCodes(query, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<TaxCodeDto> createTaxCode(@Valid @RequestBody CreateTaxCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taxCodeService.createTaxCode(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<TaxCodeDto> updateTaxCode(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaxCodeRequest request) {
        return ResponseEntity.ok(taxCodeService.updateTaxCode(id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<TaxCodeDto> activateTaxCode(@PathVariable Long id) {
        return ResponseEntity.ok(taxCodeService.activateTaxCode(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<TaxCodeDto> deactivateTaxCode(@PathVariable Long id) {
        return ResponseEntity.ok(taxCodeService.deactivateTaxCode(id));
    }

    // Tax Rates
    @GetMapping("/{taxCodeId}/rates")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<List<TaxRateDto>> getTaxRates(@PathVariable Long taxCodeId) {
        return ResponseEntity.ok(taxCodeService.getTaxRates(taxCodeId));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<TaxRateDto> addTaxRate(@Valid @RequestBody CreateTaxRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taxCodeService.addTaxRate(request));
    }

    @PutMapping("/rates/{rateId}")
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<TaxRateDto> updateTaxRate(
            @PathVariable Long rateId,
            @Valid @RequestBody CreateTaxRateRequest request) {
        return ResponseEntity.ok(taxCodeService.updateTaxRate(rateId, request));
    }

    @DeleteMapping("/rates/{rateId}")
    @PreAuthorize("hasAuthority('FINANCE_TAX_MANAGE')")
    public ResponseEntity<Void> deleteTaxRate(@PathVariable Long rateId) {
        taxCodeService.deleteTaxRate(rateId);
        return ResponseEntity.noContent().build();
    }

    // Tax Calculations
    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('FINANCE_TAX_VIEW')")
    public ResponseEntity<TaxCalculationResult> calculateTax(@Valid @RequestBody TaxCalculationRequest request) {
        return ResponseEntity.ok(taxCalculationService.calculateTax(request));
    }

    // Tax Reports
    @GetMapping("/reports/vat-summary")
    @PreAuthorize("hasAuthority('FINANCE_TAX_REPORTS')")
    public ResponseEntity<TaxReportDto> getVatSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(taxReportService.generateVatSummaryReport(periodStart, periodEnd));
    }

    @GetMapping("/reports/summary-by-type")
    @PreAuthorize("hasAuthority('FINANCE_TAX_REPORTS')")
    public ResponseEntity<List<TaxReportDto>> getTaxSummaryByType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(taxReportService.generateTaxSummaryByType(periodStart, periodEnd));
    }
}
