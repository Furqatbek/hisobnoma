package com.hisobnoma.platform.hr.controller;

import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.service.SalaryAdvanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/advances")
@RequiredArgsConstructor
public class SalaryAdvanceController {

    private final SalaryAdvanceService advanceService;

    @GetMapping("/period")
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<List<SalaryAdvanceDto>> getByPeriod(
            @RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(advanceService.getByPeriod(year, month));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<List<SalaryAdvanceDto>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(advanceService.getByEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/total")
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<BigDecimal> getUndeductedTotal(
            @PathVariable Long employeeId, @RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(advanceService.getUndeductedTotal(employeeId, year, month));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_SALARY_WRITE')")
    public ResponseEntity<SalaryAdvanceDto> create(@Valid @RequestBody CreateSalaryAdvanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(advanceService.create(request));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('HR_SALARY_WRITE')")
    public ResponseEntity<SalaryAdvanceDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(advanceService.cancel(id));
    }
}
