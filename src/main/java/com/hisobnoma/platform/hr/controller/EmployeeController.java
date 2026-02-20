package com.hisobnoma.platform.hr.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.hr.dto.CreateEmployeeRequest;
import com.hisobnoma.platform.hr.dto.EmployeeDto;
import com.hisobnoma.platform.hr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_READ')")
    public ResponseEntity<PageResponse<EmployeeDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAll(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_READ')")
    public ResponseEntity<List<EmployeeDto>> getActive() {
        return ResponseEntity.ok(employeeService.getActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_READ')")
    public ResponseEntity<EmployeeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_READ')")
    public ResponseEntity<PageResponse<EmployeeDto>> search(@RequestParam String q, Pageable pageable) {
        return ResponseEntity.ok(employeeService.search(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeDto> update(@PathVariable Long id, @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_WRITE')")
    public ResponseEntity<EmployeeDto> terminate(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.terminate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_EMPLOYEE_WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
