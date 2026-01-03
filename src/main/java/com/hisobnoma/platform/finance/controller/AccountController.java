package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.AccountDto;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.service.AccountService;
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
@RequestMapping("/api/v1/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<PageResponse<AccountDto>> getAccounts(
            @PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(accountService.getAccounts(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/roots")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<List<AccountDto>> getRootAccounts() {
        return ResponseEntity.ok(accountService.getRootAccounts());
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<List<AccountDto>> getChildAccounts(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getChildAccounts(id));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<List<AccountDto>> getActiveAccounts() {
        return ResponseEntity.ok(accountService.getActiveAccounts());
    }

    @GetMapping("/postable")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<List<AccountDto>> getPostableAccounts() {
        return ResponseEntity.ok(accountService.getPostableAccounts());
    }

    @GetMapping("/type/{accountType}")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<List<AccountDto>> getAccountsByType(@PathVariable AccountType accountType) {
        return ResponseEntity.ok(accountService.getAccountsByType(accountType));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/{id}/tree")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<AccountDto> getAccountWithChildren(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountWithChildren(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<AccountDto> getAccountByCode(@PathVariable String code) {
        return ResponseEntity.ok(accountService.getAccountByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('FINANCE_GL_VIEW')")
    public ResponseEntity<PageResponse<AccountDto>> searchAccounts(
            @RequestParam String q,
            @PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(accountService.searchAccounts(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_GL_MANAGE')")
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_GL_MANAGE')")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_GL_MANAGE')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_GL_MANAGE')")
    public ResponseEntity<AccountDto> activateAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.activateAccount(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FINANCE_GL_MANAGE')")
    public ResponseEntity<AccountDto> deactivateAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.deactivateAccount(id));
    }
}
