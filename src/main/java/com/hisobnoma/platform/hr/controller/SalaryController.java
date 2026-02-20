package com.hisobnoma.platform.hr.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.service.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<PageResponse<SalaryRecordDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(salaryService.getAll(pageable));
    }

    @GetMapping("/period")
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<PageResponse<SalaryRecordDto>> getByPeriod(
            @RequestParam Integer year, @RequestParam Integer month, Pageable pageable) {
        return ResponseEntity.ok(salaryService.getByPeriod(year, month, pageable));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<List<SalaryRecordDto>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryService.getByEmployee(employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_SALARY_READ')")
    public ResponseEntity<SalaryRecordDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salaryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_SALARY_WRITE')")
    public ResponseEntity<SalaryRecordDto> create(@Valid @RequestBody CreateSalaryRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaryService.create(request));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('HR_SALARY_WRITE')")
    public ResponseEntity<SalaryRecordDto> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(salaryService.markPaid(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('HR_SALARY_WRITE')")
    public ResponseEntity<SalaryRecordDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(salaryService.cancel(id));
    }
}
