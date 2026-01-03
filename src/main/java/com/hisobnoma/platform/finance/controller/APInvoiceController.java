package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.APInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceRequest;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.service.APInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ap/invoices")
@RequiredArgsConstructor
public class APInvoiceController {

    private final APInvoiceService apInvoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<PageResponse<APInvoiceDto>> getInvoices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(apInvoiceService.getInvoices(pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<PageResponse<APInvoiceDto>> getInvoicesByVendor(
            @PathVariable Long vendorId,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(apInvoiceService.getInvoicesByVendor(vendorId, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<PageResponse<APInvoiceDto>> getInvoicesByStatus(
            @PathVariable APInvoiceStatus status,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(apInvoiceService.getInvoicesByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> getInvoice(@PathVariable Long id) {
        APInvoiceDto invoice = apInvoiceService.getInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/vendor/{vendorId}/unpaid")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<List<APInvoiceDto>>> getUnpaidInvoicesByVendor(@PathVariable Long vendorId) {
        List<APInvoiceDto> invoices = apInvoiceService.getUnpaidInvoicesByVendor(vendorId);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<List<APInvoiceDto>>> getOverdueInvoices() {
        List<APInvoiceDto> invoices = apInvoiceService.getOverdueInvoices();
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_AP_CREATE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> createInvoice(@Valid @RequestBody CreateAPInvoiceRequest request) {
        APInvoiceDto invoice = apInvoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(invoice));
    }

    @PostMapping("/from-receiving/{receivingOrderId}")
    @PreAuthorize("hasAuthority('FINANCE_AP_CREATE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> createFromReceiving(@PathVariable Long receivingOrderId) {
        APInvoiceDto invoice = apInvoiceService.createFromReceiving(receivingOrderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(invoice));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AP_UPDATE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody CreateAPInvoiceRequest request) {
        APInvoiceDto invoice = apInvoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('FINANCE_AP_CREATE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> submitForApproval(@PathVariable Long id) {
        APInvoiceDto invoice = apInvoiceService.submitForApproval(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('FINANCE_AP_APPROVE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> approveInvoice(@PathVariable Long id) {
        APInvoiceDto invoice = apInvoiceService.approveInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('FINANCE_AP_APPROVE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> rejectInvoice(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        APInvoiceDto invoice = apInvoiceService.rejectInvoice(id, reason);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_AP_CANCEL')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> cancelInvoice(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        APInvoiceDto invoice = apInvoiceService.cancelInvoice(id, reason);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('FINANCE_AP_UPDATE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> holdInvoice(@PathVariable Long id) {
        APInvoiceDto invoice = apInvoiceService.holdInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{id}/release-hold")
    @PreAuthorize("hasAuthority('FINANCE_AP_UPDATE')")
    public ResponseEntity<ApiResponse<APInvoiceDto>> releaseHold(@PathVariable Long id) {
        APInvoiceDto invoice = apInvoiceService.releaseHold(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    // Summary endpoints
    @GetMapping("/summary/total-payable")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalPayable() {
        BigDecimal total = apInvoiceService.getTotalPayable();
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/summary/vendor/{vendorId}/balance")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> getVendorBalance(@PathVariable Long vendorId) {
        BigDecimal balance = apInvoiceService.getVendorBalance(vendorId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @GetMapping("/summary/overdue-balance")
    @PreAuthorize("hasAuthority('FINANCE_AP_VIEW')")
    public ResponseEntity<ApiResponse<BigDecimal>> getOverdueBalance() {
        BigDecimal balance = apInvoiceService.getOverdueBalance();
        return ResponseEntity.ok(ApiResponse.success(balance));
    }
}
