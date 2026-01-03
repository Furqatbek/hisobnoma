package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
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
@RequestMapping("/api/v1/finance/ar-invoices")
@RequiredArgsConstructor
public class ARInvoiceController {

    private final ARInvoiceService arInvoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<ARInvoiceDto>> getInvoices(Pageable pageable) {
        return ResponseEntity.ok(arInvoiceService.getInvoices(pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<ARInvoiceDto>> getInvoicesByCustomer(
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(arInvoiceService.getInvoicesByCustomer(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<ARInvoiceDto>> getInvoicesByStatus(
            @PathVariable ARInvoiceStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(arInvoiceService.getInvoicesByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<ARInvoiceDto> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(arInvoiceService.getInvoice(id));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<ARInvoiceDto> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(arInvoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping("/customer/{customerId}/unpaid")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<ARInvoiceDto>> getUnpaidInvoicesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(arInvoiceService.getUnpaidInvoicesByCustomer(customerId));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<ARInvoiceDto>> getOverdueInvoices() {
        return ResponseEntity.ok(arInvoiceService.getOverdueInvoices());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARInvoiceDto> createInvoice(@Valid @RequestBody CreateARInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(arInvoiceService.createInvoice(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARInvoiceDto> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody CreateARInvoiceRequest request) {
        return ResponseEntity.ok(arInvoiceService.updateInvoice(id, request));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('FINANCE_AR_APPROVE')")
    public ResponseEntity<ARInvoiceDto> postInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(arInvoiceService.postInvoice(id));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<ARInvoiceDto> sendInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(arInvoiceService.sendInvoice(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_AR_APPROVE')")
    public ResponseEntity<ARInvoiceDto> cancelInvoice(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(arInvoiceService.cancelInvoice(id, reason));
    }

    @GetMapping("/customer/{customerId}/outstanding")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<BigDecimal> getCustomerTotalOutstanding(@PathVariable Long customerId) {
        return ResponseEntity.ok(arInvoiceService.getCustomerTotalOutstanding(customerId));
    }

    @PostMapping("/check-overdue")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<Void> checkOverdueInvoices() {
        arInvoiceService.checkOverdueInvoices();
        return ResponseEntity.ok().build();
    }
}
