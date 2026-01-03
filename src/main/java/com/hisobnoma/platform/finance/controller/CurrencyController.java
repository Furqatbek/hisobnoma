package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateCurrencyRequest;
import com.hisobnoma.platform.finance.dto.CurrencyDto;
import com.hisobnoma.platform.finance.service.CurrencyService;
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
@RequestMapping("/api/v1/finance/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<PageResponse<CurrencyDto>> getCurrencies(
            @PageableDefault(sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(currencyService.getCurrencies(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<List<CurrencyDto>> getActiveCurrencies() {
        return ResponseEntity.ok(currencyService.getActiveCurrencies());
    }

    @GetMapping("/base")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<CurrencyDto> getBaseCurrency() {
        return ResponseEntity.ok(currencyService.getBaseCurrency());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<CurrencyDto> getCurrency(@PathVariable Long id) {
        return ResponseEntity.ok(currencyService.getCurrency(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<CurrencyDto> getCurrencyByCode(@PathVariable String code) {
        return ResponseEntity.ok(currencyService.getCurrencyByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<CurrencyDto> createCurrency(@Valid @RequestBody CreateCurrencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(currencyService.createCurrency(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<CurrencyDto> updateCurrency(
            @PathVariable Long id,
            @Valid @RequestBody CreateCurrencyRequest request) {
        return ResponseEntity.ok(currencyService.updateCurrency(id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<CurrencyDto> activateCurrency(@PathVariable Long id) {
        return ResponseEntity.ok(currencyService.activateCurrency(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<CurrencyDto> deactivateCurrency(@PathVariable Long id) {
        return ResponseEntity.ok(currencyService.deactivateCurrency(id));
    }

    @PutMapping("/{id}/set-base")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<CurrencyDto> setBaseCurrency(@PathVariable Long id) {
        return ResponseEntity.ok(currencyService.setBaseCurrency(id));
    }
}
