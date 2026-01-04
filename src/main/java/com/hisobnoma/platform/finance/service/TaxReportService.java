package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.TaxReportDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.JournalLineRepository;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaxReportService {

    private final TaxCodeRepository taxCodeRepository;
    private final JournalLineRepository journalLineRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Generates a VAT/GST summary report for the given period.
     */
    @Transactional(readOnly = true)
    public TaxReportDto generateVatSummaryReport(LocalDate periodStart, LocalDate periodEnd) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Get all VAT tax codes
        List<TaxCode> vatCodes = taxCodeRepository.findByTaxTypeAndTenantId(TaxType.VAT, tenantId);

        Map<String, TaxReportDto.TaxReportLineDto> lineMap = new HashMap<>();
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalSalesTax = BigDecimal.ZERO;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalPurchaseTax = BigDecimal.ZERO;

        for (TaxCode taxCode : vatCodes) {
            BigDecimal salesAmount = BigDecimal.ZERO;
            BigDecimal salesTax = BigDecimal.ZERO;
            BigDecimal purchaseAmount = BigDecimal.ZERO;
            BigDecimal purchaseTax = BigDecimal.ZERO;

            // Get the current effective rate
            BigDecimal rate = BigDecimal.ZERO;
            if (!taxCode.getRates().isEmpty()) {
                for (TaxRate taxRate : taxCode.getRates()) {
                    if (taxRate.isEffectiveFor(periodEnd)) {
                        rate = taxRate.getRate();
                        break;
                    }
                }
            }

            // Calculate sales tax from journal lines with this tax code
            if (taxCode.getSalesAccount() != null) {
                List<JournalLine> salesLines = journalLineRepository.findByTaxCodeAndAccountIdAndDateRange(
                        taxCode.getCode(), taxCode.getSalesAccount().getId(), periodStart, periodEnd, tenantId);

                for (JournalLine line : salesLines) {
                    salesTax = salesTax.add(line.getCreditAmount().subtract(line.getDebitAmount()));
                }

                // Estimate sales amount from tax (tax / rate * 100)
                if (rate.compareTo(BigDecimal.ZERO) > 0) {
                    salesAmount = salesTax.multiply(BigDecimal.valueOf(100)).divide(rate, 2, java.math.RoundingMode.HALF_UP);
                }
            }

            // Calculate purchase tax from journal lines with this tax code
            if (taxCode.getPurchaseAccount() != null) {
                List<JournalLine> purchaseLines = journalLineRepository.findByTaxCodeAndAccountIdAndDateRange(
                        taxCode.getCode(), taxCode.getPurchaseAccount().getId(), periodStart, periodEnd, tenantId);

                for (JournalLine line : purchaseLines) {
                    purchaseTax = purchaseTax.add(line.getDebitAmount().subtract(line.getCreditAmount()));
                }

                // Estimate purchase amount from tax
                if (rate.compareTo(BigDecimal.ZERO) > 0) {
                    purchaseAmount = purchaseTax.multiply(BigDecimal.valueOf(100)).divide(rate, 2, java.math.RoundingMode.HALF_UP);
                }
            }

            BigDecimal netTax = salesTax.subtract(purchaseTax);

            TaxReportDto.TaxReportLineDto lineDto = TaxReportDto.TaxReportLineDto.builder()
                    .taxCode(taxCode.getCode())
                    .taxName(taxCode.getName())
                    .rate(rate)
                    .salesAmount(salesAmount)
                    .salesTax(salesTax)
                    .purchaseAmount(purchaseAmount)
                    .purchaseTax(purchaseTax)
                    .netTax(netTax)
                    .build();

            lineMap.put(taxCode.getCode(), lineDto);

            totalSales = totalSales.add(salesAmount);
            totalSalesTax = totalSalesTax.add(salesTax);
            totalPurchases = totalPurchases.add(purchaseAmount);
            totalPurchaseTax = totalPurchaseTax.add(purchaseTax);
        }

        BigDecimal netTaxPayable = totalSalesTax.subtract(totalPurchaseTax);

        return TaxReportDto.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .taxType("VAT")
                .totalSales(totalSales)
                .totalSalesTax(totalSalesTax)
                .totalPurchases(totalPurchases)
                .totalPurchaseTax(totalPurchaseTax)
                .netTaxPayable(netTaxPayable)
                .lines(new ArrayList<>(lineMap.values()))
                .build();
    }

    /**
     * Generates a general tax summary report for all tax types.
     */
    @Transactional(readOnly = true)
    public List<TaxReportDto> generateTaxSummaryByType(LocalDate periodStart, LocalDate periodEnd) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<TaxReportDto> reports = new ArrayList<>();

        for (TaxType type : TaxType.values()) {
            List<TaxCode> codes = taxCodeRepository.findByTaxTypeAndTenantId(type, tenantId);
            if (!codes.isEmpty()) {
                TaxReportDto report = generateReportForType(type, codes, periodStart, periodEnd, tenantId);
                if (report.getLines() != null && !report.getLines().isEmpty()) {
                    reports.add(report);
                }
            }
        }

        return reports;
    }

    private TaxReportDto generateReportForType(TaxType type, List<TaxCode> codes,
                                                LocalDate periodStart, LocalDate periodEnd, Long tenantId) {
        List<TaxReportDto.TaxReportLineDto> lines = new ArrayList<>();
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalSalesTax = BigDecimal.ZERO;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalPurchaseTax = BigDecimal.ZERO;

        for (TaxCode taxCode : codes) {
            BigDecimal salesTax = BigDecimal.ZERO;
            BigDecimal purchaseTax = BigDecimal.ZERO;

            BigDecimal rate = BigDecimal.ZERO;
            if (!taxCode.getRates().isEmpty()) {
                for (TaxRate taxRate : taxCode.getRates()) {
                    if (taxRate.isEffectiveFor(periodEnd)) {
                        rate = taxRate.getRate();
                        break;
                    }
                }
            }

            if (taxCode.getSalesAccount() != null) {
                List<JournalLine> salesLines = journalLineRepository.findByTaxCodeAndAccountIdAndDateRange(
                        taxCode.getCode(), taxCode.getSalesAccount().getId(), periodStart, periodEnd, tenantId);
                for (JournalLine line : salesLines) {
                    salesTax = salesTax.add(line.getCreditAmount().subtract(line.getDebitAmount()));
                }
            }

            if (taxCode.getPurchaseAccount() != null) {
                List<JournalLine> purchaseLines = journalLineRepository.findByTaxCodeAndAccountIdAndDateRange(
                        taxCode.getCode(), taxCode.getPurchaseAccount().getId(), periodStart, periodEnd, tenantId);
                for (JournalLine line : purchaseLines) {
                    purchaseTax = purchaseTax.add(line.getDebitAmount().subtract(line.getCreditAmount()));
                }
            }

            BigDecimal salesAmount = rate.compareTo(BigDecimal.ZERO) > 0 ?
                    salesTax.multiply(BigDecimal.valueOf(100)).divide(rate, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal purchaseAmount = rate.compareTo(BigDecimal.ZERO) > 0 ?
                    purchaseTax.multiply(BigDecimal.valueOf(100)).divide(rate, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

            if (salesTax.compareTo(BigDecimal.ZERO) != 0 || purchaseTax.compareTo(BigDecimal.ZERO) != 0) {
                lines.add(TaxReportDto.TaxReportLineDto.builder()
                        .taxCode(taxCode.getCode())
                        .taxName(taxCode.getName())
                        .rate(rate)
                        .salesAmount(salesAmount)
                        .salesTax(salesTax)
                        .purchaseAmount(purchaseAmount)
                        .purchaseTax(purchaseTax)
                        .netTax(salesTax.subtract(purchaseTax))
                        .build());

                totalSales = totalSales.add(salesAmount);
                totalSalesTax = totalSalesTax.add(salesTax);
                totalPurchases = totalPurchases.add(purchaseAmount);
                totalPurchaseTax = totalPurchaseTax.add(purchaseTax);
            }
        }

        return TaxReportDto.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .taxType(type.name())
                .totalSales(totalSales)
                .totalSalesTax(totalSalesTax)
                .totalPurchases(totalPurchases)
                .totalPurchaseTax(totalPurchaseTax)
                .netTaxPayable(totalSalesTax.subtract(totalPurchaseTax))
                .lines(lines)
                .build();
    }
}
