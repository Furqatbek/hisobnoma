package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.dto.FiscalPeriodDto;
import com.hisobnoma.platform.finance.dto.FiscalYearDto;
import com.hisobnoma.platform.finance.service.FiscalPeriodService;
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
@RequestMapping("/api/v1/finance/fiscal")
@RequiredArgsConstructor
public class FiscalPeriodController {

    private final FiscalPeriodService fiscalPeriodService;

    // ============== Fiscal Year Endpoints ==============

    @GetMapping("/years")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<PageResponse<FiscalYearDto>> getFiscalYears(
            @PageableDefault(sort = "year", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(fiscalPeriodService.getFiscalYears(pageable));
    }

    @GetMapping("/years/all")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<List<FiscalYearDto>> getAllFiscalYears() {
        return ResponseEntity.ok(fiscalPeriodService.getAllFiscalYears());
    }

    @GetMapping("/years/current")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<FiscalYearDto> getCurrentFiscalYear() {
        return ResponseEntity.ok(fiscalPeriodService.getCurrentFiscalYear());
    }

    @GetMapping("/years/{id}")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<FiscalYearDto> getFiscalYear(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalPeriodService.getFiscalYear(id));
    }

    @GetMapping("/years/{id}/details")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<FiscalYearDto> getFiscalYearWithPeriods(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalPeriodService.getFiscalYearWithPeriods(id));
    }

    @GetMapping("/years/by-date")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<FiscalYearDto> getFiscalYearByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(fiscalPeriodService.getFiscalYearByDate(date));
    }

    @PostMapping("/years")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_MANAGE')")
    public ResponseEntity<FiscalYearDto> createFiscalYear(@Valid @RequestBody CreateFiscalYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fiscalPeriodService.createFiscalYear(request));
    }

    @PutMapping("/years/{id}")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_MANAGE')")
    public ResponseEntity<FiscalYearDto> updateFiscalYear(
            @PathVariable Long id,
            @Valid @RequestBody CreateFiscalYearRequest request) {
        return ResponseEntity.ok(fiscalPeriodService.updateFiscalYear(id, request));
    }

    @PutMapping("/years/{id}/set-current")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_MANAGE')")
    public ResponseEntity<FiscalYearDto> setCurrentFiscalYear(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalPeriodService.setCurrentFiscalYear(id));
    }

    @PostMapping("/years/{id}/close")
    @PreAuthorize("hasAuthority('FINANCE_YEAR_CLOSE')")
    public ResponseEntity<FiscalYearDto> closeFiscalYear(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalPeriodService.closeFiscalYear(id));
    }

    // ============== Fiscal Period Endpoints ==============

    @GetMapping("/years/{yearId}/periods")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<List<FiscalPeriodDto>> getPeriodsByFiscalYear(@PathVariable Long yearId) {
        return ResponseEntity.ok(fiscalPeriodService.getPeriodsByFiscalYear(yearId));
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<FiscalPeriodDto> getPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalPeriodService.getPeriod(id));
    }

    @GetMapping("/periods/by-date")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<FiscalPeriodDto> getPeriodByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(fiscalPeriodService.getPeriodByDate(date));
    }

    @GetMapping("/periods/open")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_VIEW')")
    public ResponseEntity<List<FiscalPeriodDto>> getOpenPeriods() {
        return ResponseEntity.ok(fiscalPeriodService.getOpenPeriods());
    }

    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_CLOSE')")
    public ResponseEntity<FiscalPeriodDto> closePeriod(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalPeriodService.closePeriod(id));
    }

    @PostMapping("/periods/{id}/reopen")
    @PreAuthorize("hasAuthority('FINANCE_PERIOD_REOPEN')")
    public ResponseEntity<FiscalPeriodDto> reopenPeriod(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(fiscalPeriodService.reopenPeriod(id, reason));
    }
}
