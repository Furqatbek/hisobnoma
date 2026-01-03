package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.dto.ExchangeRateDto;
import com.hisobnoma.platform.finance.entity.ExchangeRate;
import com.hisobnoma.platform.finance.mapper.ExchangeRateMapper;
import com.hisobnoma.platform.finance.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<ExchangeRateDto> getExchangeRates(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<ExchangeRate> page = exchangeRateRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(exchangeRateMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> getAllExchangeRates() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ExchangeRate> rates = exchangeRateRepository.findAllByTenantId(tenantId);
        return exchangeRateMapper.toDtoList(rates);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> getCurrentActiveRates() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ExchangeRate> rates = exchangeRateRepository.findCurrentActiveRates(tenantId);
        return exchangeRateMapper.toDtoList(rates);
    }

    @Transactional(readOnly = true)
    public ExchangeRateDto getExchangeRate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ExchangeRate rate = exchangeRateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Exchange rate not found with id: " + id));
        return exchangeRateMapper.toDto(rate);
    }

    @Transactional(readOnly = true)
    public ExchangeRateDto getEffectiveRate(String fromCurrency, String toCurrency, LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // If converting to the same currency, return rate of 1
        if (fromCurrency.equals(toCurrency)) {
            return ExchangeRateDto.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(BigDecimal.ONE)
                    .inverseRate(BigDecimal.ONE)
                    .effectiveDate(date)
                    .active(true)
                    .build();
        }

        ExchangeRate rate = exchangeRateRepository.findEffectiveRate(tenantId, fromCurrency, toCurrency, date)
                .orElseThrow(() -> new NotFoundException("No exchange rate found for " + fromCurrency + " to " + toCurrency + " on " + date));
        return exchangeRateMapper.toDto(rate);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> getRatesByCurrencyPair(String fromCurrency, String toCurrency) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ExchangeRate> rates = exchangeRateRepository.findByFromCurrencyAndToCurrency(tenantId, fromCurrency, toCurrency);
        return exchangeRateMapper.toDtoList(rates);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> getRatesByDate(LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<ExchangeRate> rates = exchangeRateRepository.findByEffectiveDate(tenantId, date);
        return exchangeRateMapper.toDtoList(rates);
    }

    @Transactional
    public ExchangeRateDto createExchangeRate(CreateExchangeRateRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (request.getFromCurrency().equals(request.getToCurrency())) {
            throw new BusinessException("From and To currencies must be different");
        }

        ExchangeRate rate = exchangeRateMapper.toEntity(request);
        rate.setTenantId(tenantId);
        rate.setActive(true);
        rate.calculateInverseRate();

        rate = exchangeRateRepository.save(rate);
        return exchangeRateMapper.toDto(rate);
    }

    @Transactional
    public ExchangeRateDto updateExchangeRate(Long id, CreateExchangeRateRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ExchangeRate rate = exchangeRateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Exchange rate not found with id: " + id));

        if (request.getFromCurrency().equals(request.getToCurrency())) {
            throw new BusinessException("From and To currencies must be different");
        }

        exchangeRateMapper.updateEntity(request, rate);
        rate.calculateInverseRate();

        rate = exchangeRateRepository.save(rate);
        return exchangeRateMapper.toDto(rate);
    }

    @Transactional
    public ExchangeRateDto activateExchangeRate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ExchangeRate rate = exchangeRateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Exchange rate not found with id: " + id));

        rate.setActive(true);
        rate = exchangeRateRepository.save(rate);
        return exchangeRateMapper.toDto(rate);
    }

    @Transactional
    public ExchangeRateDto deactivateExchangeRate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ExchangeRate rate = exchangeRateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Exchange rate not found with id: " + id));

        rate.setActive(false);
        rate = exchangeRateRepository.save(rate);
        return exchangeRateMapper.toDto(rate);
    }

    @Transactional
    public void deleteExchangeRate(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        ExchangeRate rate = exchangeRateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Exchange rate not found with id: " + id));

        exchangeRateRepository.delete(rate);
    }

    /**
     * Converts an amount from one currency to another using the rate for a specific date.
     */
    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate date) {
        if (amount == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }

        ExchangeRateDto rate = getEffectiveRate(fromCurrency, toCurrency, date);
        return amount.multiply(rate.getRate());
    }

    /**
     * Gets the exchange rate value for a currency pair on a specific date.
     */
    @Transactional(readOnly = true)
    public BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate date) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        ExchangeRateDto rate = getEffectiveRate(fromCurrency, toCurrency, date);
        return rate.getRate();
    }
}
