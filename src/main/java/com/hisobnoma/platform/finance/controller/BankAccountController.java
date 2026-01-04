package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.BankAccountDto;
import com.hisobnoma.platform.finance.dto.CreateBankAccountRequest;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import com.hisobnoma.platform.finance.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<PageResponse<BankAccountDto>> getBankAccounts(
            @PageableDefault(sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bankAccountService.getBankAccounts(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<List<BankAccountDto>> getAllBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllBankAccounts());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<List<BankAccountDto>> getActiveBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getActiveBankAccounts());
    }

    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<List<BankAccountDto>> getBankAccountsByType(@PathVariable BankAccountType type) {
        return ResponseEntity.ok(bankAccountService.getBankAccountsByType(type));
    }

    @GetMapping("/payment-accounts")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<List<BankAccountDto>> getPaymentAccounts() {
        return ResponseEntity.ok(bankAccountService.getPaymentAccounts());
    }

    @GetMapping("/receipt-accounts")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<List<BankAccountDto>> getReceiptAccounts() {
        return ResponseEntity.ok(bankAccountService.getReceiptAccounts());
    }

    @GetMapping("/default")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<BankAccountDto> getDefaultBankAccount() {
        return ResponseEntity.ok(bankAccountService.getDefaultBankAccount());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<BankAccountDto> getBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getBankAccount(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<BankAccountDto> getBankAccountByCode(@PathVariable String code) {
        return ResponseEntity.ok(bankAccountService.getBankAccountByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('FINANCE_BANK_VIEW')")
    public ResponseEntity<PageResponse<BankAccountDto>> searchBankAccounts(
            @RequestParam String query,
            @PageableDefault(sort = "accountCode", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(bankAccountService.searchBankAccounts(query, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_BANK_MANAGE')")
    public ResponseEntity<BankAccountDto> createBankAccount(@Valid @RequestBody CreateBankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bankAccountService.createBankAccount(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_BANK_MANAGE')")
    public ResponseEntity<BankAccountDto> updateBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateBankAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.updateBankAccount(id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_BANK_MANAGE')")
    public ResponseEntity<BankAccountDto> activateBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.activateBankAccount(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FINANCE_BANK_MANAGE')")
    public ResponseEntity<BankAccountDto> deactivateBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.deactivateBankAccount(id));
    }

    @PutMapping("/{id}/set-default")
    @PreAuthorize("hasAuthority('FINANCE_BANK_MANAGE')")
    public ResponseEntity<BankAccountDto> setDefaultBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.setDefaultBankAccount(id));
    }
}
