package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.APPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentRequest;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
import com.hisobnoma.platform.finance.service.APPaymentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ap/payments")
@RequiredArgsConstructor
public class APPaymentController {

    private final APPaymentService apPaymentService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<PageResponse<APPaymentDto>> getPayments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(apPaymentService.getPayments(pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<PageResponse<APPaymentDto>> getPaymentsByVendor(
            @PathVariable Long vendorId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(apPaymentService.getPaymentsByVendor(vendorId, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<PageResponse<APPaymentDto>> getPaymentsByStatus(
            @PathVariable APPaymentStatus status,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(apPaymentService.getPaymentsByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<ApiResponse<APPaymentDto>> getPayment(@PathVariable Long id) {
        APPaymentDto payment = apPaymentService.getPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<ApiResponse<List<APPaymentDto>>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<APPaymentDto> payments = apPaymentService.getPaymentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/unreconciled")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<ApiResponse<List<APPaymentDto>>> getUnreconciledPayments() {
        List<APPaymentDto> payments = apPaymentService.getUnreconciledPayments();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY')")
    public ResponseEntity<ApiResponse<APPaymentDto>> createPayment(@Valid @RequestBody CreateAPPaymentRequest request) {
        APPaymentDto payment = apPaymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY')")
    public ResponseEntity<ApiResponse<APPaymentDto>> submitForApproval(@PathVariable Long id) {
        APPaymentDto payment = apPaymentService.submitForApproval(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_APPROVE')")
    public ResponseEntity<ApiResponse<APPaymentDto>> approvePayment(@PathVariable Long id) {
        APPaymentDto payment = apPaymentService.approvePayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY')")
    public ResponseEntity<ApiResponse<APPaymentDto>> processPayment(@PathVariable Long id) {
        APPaymentDto payment = apPaymentService.processPayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VOID')")
    public ResponseEntity<ApiResponse<APPaymentDto>> voidPayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        APPaymentDto payment = apPaymentService.voidPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY')")
    public ResponseEntity<ApiResponse<APPaymentDto>> reconcilePayment(@PathVariable Long id) {
        APPaymentDto payment = apPaymentService.reconcilePayment(id);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    // Summary endpoints
    @GetMapping("/summary/vendor/{vendorId}/total")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPaymentsByVendor(@PathVariable Long vendorId) {
        BigDecimal total = apPaymentService.getTotalPaymentsByVendor(vendorId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/summary/date-range/total")
    @PreAuthorize("hasAuthority('FINANCE_AP_PAY_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal total = apPaymentService.getTotalPaymentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(total));
    }
}
