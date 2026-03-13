package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<CustomerDto>> getCustomers(Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomers(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<CustomerDto>> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getActiveCustomers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<CustomerDto> getCustomerByCode(@PathVariable String code) {
        return ResponseEntity.ok(customerService.getCustomerByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<CustomerDto>> searchCustomers(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(query, pageable));
    }

    @GetMapping("/next-code")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<String> getNextCustomerCode() {
        return ResponseEntity.ok(customerService.getNextCustomerCode());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CustomerDto> activateCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.activateCustomer(id));
    }

    @PatchMapping("/{id}/credit-hold")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CustomerDto> setCreditHold(
            @PathVariable Long id,
            @RequestParam boolean hold) {
        return ResponseEntity.ok(customerService.setCreditHold(id, hold));
    }

    @PatchMapping("/{id}/credit-limit")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CustomerDto> updateCreditLimit(
            @PathVariable Long id,
            @RequestParam BigDecimal creditLimit) {
        return ResponseEntity.ok(customerService.updateCreditLimit(id, creditLimit));
    }

    @GetMapping("/credit-hold")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<CustomerDto>> getCustomersOnCreditHold() {
        return ResponseEntity.ok(customerService.getCustomersOnCreditHold());
    }

    @GetMapping("/over-credit-limit")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<CustomerDto>> getCustomersOverCreditLimit() {
        return ResponseEntity.ok(customerService.getCustomersOverCreditLimit());
    }

    @GetMapping("/{id}/can-invoice")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<Boolean> canBeInvoiced(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(customerService.canBeInvoiced(id, amount));
    }
}
