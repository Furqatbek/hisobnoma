package com.hisobnoma.platform.finance.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.dto.ExchangeRateDto;
import com.hisobnoma.platform.finance.service.ExchangeRateService;
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
@RequestMapping("/api/v1/finance/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<PageResponse<ExchangeRateDto>> getExchangeRates(
            @PageableDefault(sort = "effectiveDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRates(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<List<ExchangeRateDto>> getAllExchangeRates() {
        return ResponseEntity.ok(exchangeRateService.getAllExchangeRates());
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<List<ExchangeRateDto>> getCurrentActiveRates() {
        return ResponseEntity.ok(exchangeRateService.getCurrentActiveRates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<ExchangeRateDto> getExchangeRate(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRate(id));
    }

    @GetMapping("/effective")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<ExchangeRateDto> getEffectiveRate(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.getEffectiveRate(from, to, date));
    }

    @GetMapping("/pair")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<List<ExchangeRateDto>> getRatesByCurrencyPair(
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(exchangeRateService.getRatesByCurrencyPair(from, to));
    }

    @GetMapping("/by-date")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<List<ExchangeRateDto>> getRatesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.getRatesByDate(date));
    }

    @GetMapping("/convert")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_VIEW')")
    public ResponseEntity<BigDecimal> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(exchangeRateService.convert(amount, from, to, date));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<ExchangeRateDto> createExchangeRate(@Valid @RequestBody CreateExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exchangeRateService.createExchangeRate(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<ExchangeRateDto> updateExchangeRate(
            @PathVariable Long id,
            @Valid @RequestBody CreateExchangeRateRequest request) {
        return ResponseEntity.ok(exchangeRateService.updateExchangeRate(id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<ExchangeRateDto> activateExchangeRate(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRateService.activateExchangeRate(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<ExchangeRateDto> deactivateExchangeRate(@PathVariable Long id) {
        return ResponseEntity.ok(exchangeRateService.deactivateExchangeRate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_CURRENCY_MANAGE')")
    public ResponseEntity<Void> deleteExchangeRate(@PathVariable Long id) {
        exchangeRateService.deleteExchangeRate(id);
        return ResponseEntity.noContent().build();
    }
}
