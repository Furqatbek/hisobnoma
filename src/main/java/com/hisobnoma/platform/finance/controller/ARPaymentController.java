package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARPaymentStatus;
import com.hisobnoma.platform.finance.service.ARPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/ar-payments")
@RequiredArgsConstructor
public class ARPaymentController {

    private final ARPaymentService arPaymentService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<ARPaymentDto>> getPayments(Pageable pageable) {
        return ResponseEntity.ok(arPaymentService.getPayments(pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<ARPaymentDto>> getPaymentsByCustomer(
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(arPaymentService.getPaymentsByCustomer(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<ARPaymentDto>> getPaymentsByStatus(
            @PathVariable ARPaymentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(arPaymentService.getPaymentsByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<ARPaymentDto> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(arPaymentService.getPayment(id));
    }

    @GetMapping("/number/{paymentNumber}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<ARPaymentDto> getPaymentByNumber(@PathVariable String paymentNumber) {
        return ResponseEntity.ok(arPaymentService.getPaymentByNumber(paymentNumber));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<ARPaymentDto>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(arPaymentService.getPaymentsByDateRange(startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARPaymentDto> createPayment(@Valid @RequestBody CreateARPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arPaymentService.createPayment(request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('FINANCE_AR_APPROVE')")
    public ResponseEntity<ARPaymentDto> completePayment(@PathVariable Long id) {
        return ResponseEntity.ok(arPaymentService.completePayment(id));
    }

    @PostMapping("/{id}/allocations")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARPaymentDto> addAllocation(
            @PathVariable Long id,
            @Valid @RequestBody CreateARPaymentAllocationRequest request) {
        return ResponseEntity.ok(arPaymentService.addAllocation(id, request));
    }

    @DeleteMapping("/{id}/allocations/{allocationId}")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARPaymentDto> removeAllocation(
            @PathVariable Long id,
            @PathVariable Long allocationId) {
        return ResponseEntity.ok(arPaymentService.removeAllocation(id, allocationId));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_AR_APPROVE')")
    public ResponseEntity<ARPaymentDto> cancelPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(arPaymentService.cancelPayment(id, reason));
    }

    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARPaymentDto> markAsDeposited(
            @PathVariable Long id,
            @RequestParam String bankReference) {
        return ResponseEntity.ok(arPaymentService.markAsDeposited(id, bankReference));
    }

    @GetMapping("/undeposited")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<ARPaymentDto>> getUndepositedPayments() {
        return ResponseEntity.ok(arPaymentService.getUndepositedPayments());
    }

    @GetMapping("/total-by-date-range")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<BigDecimal> getTotalPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(arPaymentService.getTotalPaymentsByDateRange(startDate, endDate));
    }
}
