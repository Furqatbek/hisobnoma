package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.*;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.entity.TaxType;
import com.hisobnoma.platform.finance.mapper.TaxCodeMapper;
import com.hisobnoma.platform.finance.mapper.TaxRateMapper;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import com.hisobnoma.platform.finance.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCodeService {

    private final TaxCodeRepository taxCodeRepository;
    private final TaxRateRepository taxRateRepository;
    private final AccountRepository accountRepository;
    private final TaxCodeMapper taxCodeMapper;
    private final TaxRateMapper taxRateMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<TaxCodeDto> getTaxCodes(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<TaxCode> page = taxCodeRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(taxCodeMapper::toDtoWithoutRates));
    }

    @Transactional(readOnly = true)
    public List<TaxCodeDto> getAllTaxCodes() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxCode> codes = taxCodeRepository.findAllByTenantId(tenantId);
        return taxCodeMapper.toDtoList(codes);
    }

    @Transactional(readOnly = true)
    public List<TaxCodeDto> getActiveTaxCodes() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxCode> codes = taxCodeRepository.findAllActiveByTenantId(tenantId);
        return taxCodeMapper.toDtoList(codes);
    }

    @Transactional(readOnly = true)
    public List<TaxCodeDto> getTaxCodesByType(TaxType type) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxCode> codes = taxCodeRepository.findByTaxTypeAndTenantId(type, tenantId);
        return taxCodeMapper.toDtoList(codes);
    }

    @Transactional(readOnly = true)
    public List<TaxCodeDto> getSalesTaxCodes() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxCode> codes = taxCodeRepository.findSalesTaxCodesByTenantId(tenantId);
        return taxCodeMapper.toDtoList(codes);
    }

    @Transactional(readOnly = true)
    public List<TaxCodeDto> getPurchaseTaxCodes() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxCode> codes = taxCodeRepository.findPurchaseTaxCodesByTenantId(tenantId);
        return taxCodeMapper.toDtoList(codes);
    }

    @Transactional(readOnly = true)
    public TaxCodeDto getTaxCode(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        TaxCode taxCode = taxCodeRepository.findByIdWithRatesAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found with id: " + id));
        return taxCodeMapper.toDto(taxCode);
    }

    @Transactional(readOnly = true)
    public TaxCodeDto getTaxCodeByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        TaxCode taxCode = taxCodeRepository.findByCodeWithRatesAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found with code: " + code));
        return taxCodeMapper.toDto(taxCode);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaxCodeDto> searchTaxCodes(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<TaxCode> page = taxCodeRepository.searchByTenantId(search, tenantId, pageable);
        return PageResponse.of(page.map(taxCodeMapper::toDtoWithoutRates));
    }

    @Transactional
    public TaxCodeDto createTaxCode(CreateTaxCodeRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check if code already exists
        if (taxCodeRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
            taxCodeRepository.existsByCodeAndTenantIdIsNull(request.getCode())) {
            throw new BusinessException("Tax code '" + request.getCode() + "' already exists");
        }

        TaxCode taxCode = taxCodeMapper.toEntity(request);
        taxCode.setTenantId(tenantId);
        taxCode.setActive(true);
        taxCode.setSystemCode(false);

        // Link GL accounts if provided
        if (request.getSalesAccountId() != null) {
            Account salesAccount = accountRepository.findByIdAndTenantId(request.getSalesAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Sales account not found"));
            taxCode.setSalesAccount(salesAccount);
        }

        if (request.getPurchaseAccountId() != null) {
            Account purchaseAccount = accountRepository.findByIdAndTenantId(request.getPurchaseAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Purchase account not found"));
            taxCode.setPurchaseAccount(purchaseAccount);
        }

        taxCode = taxCodeRepository.save(taxCode);
        return taxCodeMapper.toDto(taxCode);
    }

    @Transactional
    public TaxCodeDto updateTaxCode(Long id, CreateTaxCodeRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        TaxCode taxCode = taxCodeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found with id: " + id));

        // Cannot update system codes
        if (taxCode.isSystemCode()) {
            throw new BusinessException("System tax codes cannot be modified");
        }

        // Check for duplicate code if changed
        if (!taxCode.getCode().equals(request.getCode())) {
            if (taxCodeRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
                taxCodeRepository.existsByCodeAndTenantIdIsNull(request.getCode())) {
                throw new BusinessException("Tax code '" + request.getCode() + "' already exists");
            }
        }

        taxCodeMapper.updateEntity(request, taxCode);

        // Update GL accounts
        if (request.getSalesAccountId() != null) {
            Account salesAccount = accountRepository.findByIdAndTenantId(request.getSalesAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Sales account not found"));
            taxCode.setSalesAccount(salesAccount);
        } else {
            taxCode.setSalesAccount(null);
        }

        if (request.getPurchaseAccountId() != null) {
            Account purchaseAccount = accountRepository.findByIdAndTenantId(request.getPurchaseAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Purchase account not found"));
            taxCode.setPurchaseAccount(purchaseAccount);
        } else {
            taxCode.setPurchaseAccount(null);
        }

        taxCode = taxCodeRepository.save(taxCode);
        return taxCodeMapper.toDto(taxCode);
    }

    @Transactional
    public TaxCodeDto activateTaxCode(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        TaxCode taxCode = taxCodeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found with id: " + id));

        taxCode.setActive(true);
        taxCode = taxCodeRepository.save(taxCode);
        return taxCodeMapper.toDto(taxCode);
    }

    @Transactional
    public TaxCodeDto deactivateTaxCode(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        TaxCode taxCode = taxCodeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found with id: " + id));

        if (taxCode.isSystemCode()) {
            throw new BusinessException("System tax codes cannot be deactivated");
        }

        taxCode.setActive(false);
        taxCode = taxCodeRepository.save(taxCode);
        return taxCodeMapper.toDto(taxCode);
    }

    // Tax Rate management
    @Transactional(readOnly = true)
    public List<TaxRateDto> getTaxRates(Long taxCodeId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxRate> rates = taxRateRepository.findByTaxCodeIdAndTenantId(taxCodeId, tenantId);
        return taxRateMapper.toDtoList(rates);
    }

    @Transactional
    public TaxRateDto addTaxRate(CreateTaxRateRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        TaxCode taxCode = taxCodeRepository.findByIdAndTenantId(request.getTaxCodeId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found"));

        TaxRate rate = taxRateMapper.toEntity(request);
        rate.setTenantId(tenantId);
        rate.setTaxCode(taxCode);
        rate.setActive(true);

        rate = taxRateRepository.save(rate);
        return taxRateMapper.toDto(rate);
    }

    @Transactional
    public TaxRateDto updateTaxRate(Long rateId, CreateTaxRateRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        TaxRate rate = taxRateRepository.findByIdAndTenantId(rateId, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax rate not found"));

        taxRateMapper.updateEntity(request, rate);
        rate = taxRateRepository.save(rate);
        return taxRateMapper.toDto(rate);
    }

    @Transactional
    public void deleteTaxRate(Long rateId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        TaxRate rate = taxRateRepository.findByIdAndTenantId(rateId, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax rate not found"));

        taxRateRepository.delete(rate);
    }
}
