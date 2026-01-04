package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CashFlowDto;
import com.hisobnoma.platform.finance.dto.CreateBankTransactionRequest;
import com.hisobnoma.platform.finance.service.BankTransactionService;
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

@RestController
@RequestMapping("/api/v1/finance/bank-transactions")
@RequiredArgsConstructor
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<PageResponse<BankTransactionDto>> getTransactions(
            @PageableDefault(sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bankTransactionService.getTransactions(pageable));
    }

    @GetMapping("/by-account/{bankAccountId}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<PageResponse<BankTransactionDto>> getTransactionsByBankAccount(
            @PathVariable Long bankAccountId,
            @PageableDefault(sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bankTransactionService.getTransactionsByBankAccount(bankAccountId, pageable));
    }

    @GetMapping("/by-account/{bankAccountId}/date-range")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<List<BankTransactionDto>> getTransactionsByDateRange(
            @PathVariable Long bankAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bankTransactionService.getTransactionsByDateRange(bankAccountId, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<BankTransactionDto> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(bankTransactionService.getTransaction(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_BANK_TRANSACT')")
    public ResponseEntity<BankTransactionDto> createTransaction(@Valid @RequestBody CreateBankTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankTransactionService.createTransaction(request));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('FINANCE_BANK_TRANSFER')")
    public ResponseEntity<BankTransactionDto> createTransfer(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate transactionDate,
            @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankTransactionService.createTransfer(fromAccountId, toAccountId, amount, transactionDate, description));
    }

    @PutMapping("/{id}/clear")
    @PreAuthorize("hasAuthority('FINANCE_BANK_RECONCILE')")
    public ResponseEntity<BankTransactionDto> clearTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(bankTransactionService.clearTransaction(id));
    }

    @PutMapping("/{id}/void")
    @PreAuthorize("hasAuthority('FINANCE_BANK_MANAGE')")
    public ResponseEntity<BankTransactionDto> voidTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(bankTransactionService.voidTransaction(id, reason));
    }

    @GetMapping("/cash-flow/{bankAccountId}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<CashFlowDto> getCashFlow(
            @PathVariable Long bankAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(bankTransactionService.getCashFlow(bankAccountId, startDate, endDate));
    }
}
