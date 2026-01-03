package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
import com.hisobnoma.platform.finance.dto.CreditNoteDto;
import com.hisobnoma.platform.finance.entity.CreditNoteStatus;
import com.hisobnoma.platform.finance.service.CreditNoteService;
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
@RequestMapping("/api/v1/finance/credit-notes")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<CreditNoteDto>> getCreditNotes(Pageable pageable) {
        return ResponseEntity.ok(creditNoteService.getCreditNotes(pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<CreditNoteDto>> getCreditNotesByCustomer(
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(creditNoteService.getCreditNotesByCustomer(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<PageResponse<CreditNoteDto>> getCreditNotesByStatus(
            @PathVariable CreditNoteStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(creditNoteService.getCreditNotesByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<CreditNoteDto> getCreditNote(@PathVariable Long id) {
        return ResponseEntity.ok(creditNoteService.getCreditNote(id));
    }

    @GetMapping("/number/{creditNoteNumber}")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<CreditNoteDto> getCreditNoteByNumber(@PathVariable String creditNoteNumber) {
        return ResponseEntity.ok(creditNoteService.getCreditNoteByNumber(creditNoteNumber));
    }

    @GetMapping("/customer/{customerId}/available")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<List<CreditNoteDto>> getAvailableCreditNotes(@PathVariable Long customerId) {
        return ResponseEntity.ok(creditNoteService.getAvailableCreditNotes(customerId));
    }

    @GetMapping("/customer/{customerId}/available-credit")
    @PreAuthorize("hasAuthority('FINANCE_AR_READ')")
    public ResponseEntity<BigDecimal> getAvailableCredit(@PathVariable Long customerId) {
        return ResponseEntity.ok(creditNoteService.getAvailableCredit(customerId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CreditNoteDto> createCreditNote(@Valid @RequestBody CreateCreditNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditNoteService.createCreditNote(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CreditNoteDto> updateCreditNote(
            @PathVariable Long id,
            @Valid @RequestBody CreateCreditNoteRequest request) {
        return ResponseEntity.ok(creditNoteService.updateCreditNote(id, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('FINANCE_AR_APPROVE')")
    public ResponseEntity<CreditNoteDto> approveCreditNote(@PathVariable Long id) {
        return ResponseEntity.ok(creditNoteService.approveCreditNote(id));
    }

    @PostMapping("/{id}/apply-to-invoice")
    @PreAuthorize("hasAuthority('FINANCE_AR_WRITE')")
    public ResponseEntity<CreditNoteDto> applyToInvoice(
            @PathVariable Long id,
            @RequestParam Long invoiceId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(creditNoteService.applyToInvoice(id, invoiceId, amount));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_AR_APPROVE')")
    public ResponseEntity<CreditNoteDto> cancelCreditNote(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(creditNoteService.cancelCreditNote(id, reason));
    }
}
