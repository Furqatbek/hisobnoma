package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.BankReconciliationDto;
import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.dto.ReconcileTransactionsRequest;
import com.hisobnoma.platform.finance.service.BankReconciliationService;
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
@RequestMapping("/api/v1/finance/bank-reconciliations")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<PageResponse<BankReconciliationDto>> getReconciliations(
            @PageableDefault(sort = "statementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reconciliationService.getReconciliations(pageable));
    }

    @GetMapping("/by-account/{bankAccountId}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<PageResponse<BankReconciliationDto>> getReconciliationsByBankAccount(
            @PathVariable Long bankAccountId,
            @PageableDefault(sort = "statementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reconciliationService.getReconciliationsByBankAccount(bankAccountId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> getReconciliation(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getReconciliation(id));
    }

    @GetMapping("/unreconciled/{bankAccountId}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<List<BankTransactionDto>> getUnreconciledTransactions(
            @PathVariable Long bankAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reconciliationService.getUnreconciledTransactions(bankAccountId, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> createReconciliation(
            @Valid @RequestBody CreateBankReconciliationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reconciliationService.createReconciliation(request));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> startReconciliation(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.startReconciliation(id));
    }

    @PostMapping("/reconcile-transactions")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> reconcileTransactions(
            @Valid @RequestBody ReconcileTransactionsRequest request) {
        return ResponseEntity.ok(reconciliationService.reconcileTransactions(request));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> completeReconciliation(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.completeReconciliation(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> cancelReconciliation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(reconciliationService.cancelReconciliation(id, reason));
    }
}
