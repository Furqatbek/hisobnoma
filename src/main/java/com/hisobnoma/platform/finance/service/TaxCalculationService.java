package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.TaxCalculationRequest;
import com.hisobnoma.platform.finance.dto.TaxCalculationResult;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import com.hisobnoma.platform.finance.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private final TaxCodeRepository taxCodeRepository;
    private final TaxRateRepository taxRateRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Calculates tax for a given amount using the specified tax code.
     */
    @Transactional(readOnly = true)
    public TaxCalculationResult calculateTax(TaxCalculationRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate date = request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now();

        TaxCode taxCode = taxCodeRepository.findByCodeWithRatesAndTenantId(request.getTaxCode(), tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found: " + request.getTaxCode()));

        TaxRate effectiveRate = taxRateRepository.findEffectiveRateByTaxCodeAndDate(
                        request.getTaxCode(), date, tenantId)
                .orElse(null);

        if (effectiveRate == null) {
            // No rate found, return zero tax
            return TaxCalculationResult.builder()
                    .taxCode(taxCode.getCode())
                    .taxName(taxCode.getName())
                    .rate(BigDecimal.ZERO)
                    .baseAmount(request.getAmount())
                    .taxAmount(BigDecimal.ZERO)
                    .totalAmount(request.getAmount())
                    .inclusive(false)
                    .build();
        }

        // Determine if we should use inclusive calculation
        boolean isInclusive = request.getInclusive() != null ? request.getInclusive() : taxCode.isInclusive();

        BigDecimal taxAmount = effectiveRate.calculateTax(request.getAmount(), isInclusive);
        BigDecimal baseAmount;
        BigDecimal totalAmount;

        if (isInclusive) {
            // Amount includes tax
            totalAmount = request.getAmount();
            baseAmount = request.getAmount().subtract(taxAmount);
        } else {
            // Amount excludes tax
            baseAmount = request.getAmount();
            totalAmount = request.getAmount().add(taxAmount);
        }

        return TaxCalculationResult.builder()
                .taxCode(taxCode.getCode())
                .taxName(taxCode.getName())
                .rate(effectiveRate.getRate())
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .inclusive(isInclusive)
                .build();
    }

    /**
     * Calculates tax for multiple amounts using the same tax code.
     */
    @Transactional(readOnly = true)
    public List<TaxCalculationResult> calculateTaxBatch(String taxCode, List<BigDecimal> amounts, LocalDate date, Boolean inclusive) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (date == null) {
            date = LocalDate.now();
        }

        TaxCode code = taxCodeRepository.findByCodeWithRatesAndTenantId(taxCode, tenantId)
                .orElseThrow(() -> new NotFoundException("Tax code not found: " + taxCode));

        TaxRate effectiveRate = taxRateRepository.findEffectiveRateByTaxCodeAndDate(taxCode, date, tenantId)
                .orElse(null);

        boolean isInclusive = inclusive != null ? inclusive : code.isInclusive();

        List<TaxCalculationResult> results = new ArrayList<>();
        for (BigDecimal amount : amounts) {
            TaxCalculationResult result;
            if (effectiveRate == null) {
                result = TaxCalculationResult.builder()
                        .taxCode(code.getCode())
                        .taxName(code.getName())
                        .rate(BigDecimal.ZERO)
                        .baseAmount(amount)
                        .taxAmount(BigDecimal.ZERO)
                        .totalAmount(amount)
                        .inclusive(false)
                        .build();
            } else {
                BigDecimal taxAmount = effectiveRate.calculateTax(amount, isInclusive);
                BigDecimal baseAmount;
                BigDecimal totalAmount;

                if (isInclusive) {
                    totalAmount = amount;
                    baseAmount = amount.subtract(taxAmount);
                } else {
                    baseAmount = amount;
                    totalAmount = amount.add(taxAmount);
                }

                result = TaxCalculationResult.builder()
                        .taxCode(code.getCode())
                        .taxName(code.getName())
                        .rate(effectiveRate.getRate())
                        .baseAmount(baseAmount)
                        .taxAmount(taxAmount)
                        .totalAmount(totalAmount)
                        .inclusive(isInclusive)
                        .build();
            }
            results.add(result);
        }

        return results;
    }

    /**
     * Calculates tax using multiple tax codes (compound taxes).
     */
    @Transactional(readOnly = true)
    public List<TaxCalculationResult> calculateCompoundTax(List<String> taxCodes, BigDecimal amount, LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (date == null) {
            date = LocalDate.now();
        }

        List<TaxCalculationResult> results = new ArrayList<>();
        BigDecimal currentBase = amount;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (String taxCodeStr : taxCodes) {
            TaxCode code = taxCodeRepository.findByCodeWithRatesAndTenantId(taxCodeStr, tenantId)
                    .orElseThrow(() -> new NotFoundException("Tax code not found: " + taxCodeStr));

            TaxRate effectiveRate = taxRateRepository.findEffectiveRateByTaxCodeAndDate(taxCodeStr, date, tenantId)
                    .orElse(null);

            if (effectiveRate != null) {
                // For compound taxes, calculate on the running total (base + accumulated tax)
                BigDecimal taxableAmount = code.isCompound() ? currentBase.add(totalTax) : currentBase;
                BigDecimal taxAmount = effectiveRate.calculateTax(taxableAmount, false);
                totalTax = totalTax.add(taxAmount);

                results.add(TaxCalculationResult.builder()
                        .taxCode(code.getCode())
                        .taxName(code.getName())
                        .rate(effectiveRate.getRate())
                        .baseAmount(taxableAmount)
                        .taxAmount(taxAmount)
                        .totalAmount(taxableAmount.add(taxAmount))
                        .inclusive(false)
                        .build());
            }
        }

        return results;
    }

    /**
     * Gets the current effective rate for a tax code.
     */
    @Transactional(readOnly = true)
    public BigDecimal getEffectiveRate(String taxCode, LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (date == null) {
            date = LocalDate.now();
        }

        return taxRateRepository.findEffectiveRateByTaxCodeAndDate(taxCode, date, tenantId)
                .map(TaxRate::getRate)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Gets the applicable TaxRate object for a tax code on a given date.
     */
    @Transactional(readOnly = true)
    public TaxRate getApplicableRate(String taxCode, LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (date == null) {
            date = LocalDate.now();
        }

        return taxRateRepository.findEffectiveRateByTaxCodeAndDate(taxCode, date, tenantId)
                .orElse(null);
    }

    /**
     * Extracts tax from a tax-inclusive amount.
     */
    @Transactional(readOnly = true)
    public TaxCalculationResult extractTax(String taxCode, BigDecimal inclusiveAmount, LocalDate date) {
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode(taxCode)
                .amount(inclusiveAmount)
                .transactionDate(date)
                .inclusive(true)
                .build();
        return calculateTax(request);
    }

    /**
     * Adds tax to a base amount (exclusive calculation).
     */
    @Transactional(readOnly = true)
    public TaxCalculationResult addTax(String taxCode, BigDecimal baseAmount, LocalDate date) {
        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode(taxCode)
                .amount(baseAmount)
                .transactionDate(date)
                .inclusive(false)
                .build();
        return calculateTax(request);
    }
}
