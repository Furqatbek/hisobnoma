package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCurrencyRequest;
import com.hisobnoma.platform.finance.dto.CurrencyDto;
import com.hisobnoma.platform.finance.entity.Currency;
import com.hisobnoma.platform.finance.mapper.CurrencyMapper;
import com.hisobnoma.platform.finance.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<CurrencyDto> getCurrencies(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Currency> page = currencyRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(currencyMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Currency> currencies = currencyRepository.findAllByTenantId(tenantId);
        return currencyMapper.toDtoList(currencies);
    }

    @Transactional(readOnly = true)
    public List<CurrencyDto> getActiveCurrencies() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Currency> currencies = currencyRepository.findAllActiveByTenantId(tenantId);
        return currencyMapper.toDtoList(currencies);
    }

    @Transactional(readOnly = true)
    public CurrencyDto getCurrency(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Currency currency = currencyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Currency not found with id: " + id));
        return currencyMapper.toDto(currency);
    }

    @Transactional(readOnly = true)
    public CurrencyDto getCurrencyByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Currency currency = currencyRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Currency not found with code: " + code));
        return currencyMapper.toDto(currency);
    }

    @Transactional(readOnly = true)
    public CurrencyDto getBaseCurrency() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Currency currency = currencyRepository.findBaseCurrencyByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("No base currency configured"));
        return currencyMapper.toDto(currency);
    }

    @Transactional
    public CurrencyDto createCurrency(CreateCurrencyRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check if code already exists
        if (currencyRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
            currencyRepository.existsByCodeAndTenantIdIsNull(request.getCode())) {
            throw new BusinessException("Currency with code '" + request.getCode() + "' already exists");
        }

        Currency currency = currencyMapper.toEntity(request);
        currency.setTenantId(tenantId);
        currency.setActive(true);

        // If this is set as base currency, unset any existing base currency
        if (request.isBaseCurrency()) {
            currencyRepository.findBaseCurrencyByTenantId(tenantId)
                    .ifPresent(baseCurrency -> {
                        baseCurrency.setBaseCurrency(false);
                        currencyRepository.save(baseCurrency);
                    });
        }

        currency = currencyRepository.save(currency);
        return currencyMapper.toDto(currency);
    }

    @Transactional
    public CurrencyDto updateCurrency(Long id, CreateCurrencyRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Currency currency = currencyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Currency not found with id: " + id));

        // Cannot update system currencies
        if (currency.getTenantId() == null) {
            throw new BusinessException("System currencies cannot be modified");
        }

        // Check for duplicate code if changed
        if (!currency.getCode().equals(request.getCode())) {
            if (currencyRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
                currencyRepository.existsByCodeAndTenantIdIsNull(request.getCode())) {
                throw new BusinessException("Currency with code '" + request.getCode() + "' already exists");
            }
        }

        currencyMapper.updateEntity(request, currency);

        // If this is set as base currency, unset any existing base currency
        if (request.isBaseCurrency() && !currency.isBaseCurrency()) {
            currencyRepository.findBaseCurrencyByTenantId(tenantId)
                    .ifPresent(baseCurrency -> {
                        if (!baseCurrency.getId().equals(currency.getId())) {
                            baseCurrency.setBaseCurrency(false);
                            currencyRepository.save(baseCurrency);
                        }
                    });
            currency.setBaseCurrency(true);
        }

        currency = currencyRepository.save(currency);
        return currencyMapper.toDto(currency);
    }

    @Transactional
    public CurrencyDto activateCurrency(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Currency currency = currencyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Currency not found with id: " + id));

        currency.setActive(true);
        currency = currencyRepository.save(currency);
        return currencyMapper.toDto(currency);
    }

    @Transactional
    public CurrencyDto deactivateCurrency(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Currency currency = currencyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Currency not found with id: " + id));

        if (currency.isBaseCurrency()) {
            throw new BusinessException("Base currency cannot be deactivated");
        }

        currency.setActive(false);
        currency = currencyRepository.save(currency);
        return currencyMapper.toDto(currency);
    }

    @Transactional
    public CurrencyDto setBaseCurrency(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Unset any existing base currency
        currencyRepository.findBaseCurrencyByTenantId(tenantId)
                .ifPresent(baseCurrency -> {
                    baseCurrency.setBaseCurrency(false);
                    currencyRepository.save(baseCurrency);
                });

        // Set the new base currency
        Currency currency = currencyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Currency not found with id: " + id));

        currency.setBaseCurrency(true);
        currency.setActive(true);
        currency = currencyRepository.save(currency);
        return currencyMapper.toDto(currency);
    }
}
