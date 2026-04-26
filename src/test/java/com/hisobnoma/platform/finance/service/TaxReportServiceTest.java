package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.TaxReportDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.JournalLineRepository;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxReportServiceTest {

    @Mock
    private TaxCodeRepository taxCodeRepository;

    @Mock
    private JournalLineRepository journalLineRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private TaxReportService taxReportService;

    private static final Long TENANT_ID = 1L;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void setUp() {
        periodStart = LocalDate.of(2024, 1, 1);
        periodEnd = LocalDate.of(2024, 3, 31);
    }

    @Test
    void generateVatSummaryReport_noVatCodes_returnsEmptyReport() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByTaxTypeAndTenantId(TaxType.VAT, TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        TaxReportDto result = taxReportService.generateVatSummaryReport(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        assertEquals("VAT", result.getTaxType());
        assertEquals(BigDecimal.ZERO, result.getTotalSales());
        assertEquals(BigDecimal.ZERO, result.getTotalSalesTax());
        assertEquals(BigDecimal.ZERO, result.getTotalPurchases());
        assertEquals(BigDecimal.ZERO, result.getTotalPurchaseTax());
        assertEquals(BigDecimal.ZERO, result.getNetTaxPayable());
    }

    @Test
    void generateVatSummaryReport_withVatCodes_returnsCorrectTotals() {
        // Given
        Account salesAccount = Account.builder().id(10L).code("2100").name("Sales Tax").tenantId(TENANT_ID).build();
        Account purchaseAccount = Account.builder().id(11L).code("1200").name("Input Tax").tenantId(TENANT_ID).build();

        TaxRate rate = TaxRate.builder()
                .id(1L)
                .name("Standard Rate")
                .rate(new BigDecimal("20.00"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .active(true)
                .build();

        TaxCode vatCode = TaxCode.builder()
                .id(1L)
                .code("VAT20")
                .name("VAT 20%")
                .taxType(TaxType.VAT)
                .salesAccount(salesAccount)
                .purchaseAccount(purchaseAccount)
                .rates(new ArrayList<>(List.of(rate)))
                .tenantId(TENANT_ID)
                .build();
        rate.setTaxCode(vatCode);

        JournalLine salesLine = JournalLine.builder()
                .id(1L)
                .creditAmount(new BigDecimal("200.00"))
                .debitAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        JournalLine purchaseLine = JournalLine.builder()
                .id(2L)
                .debitAmount(new BigDecimal("80.00"))
                .creditAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByTaxTypeAndTenantId(TaxType.VAT, TENANT_ID)).thenReturn(List.of(vatCode));
        when(journalLineRepository.findByTaxCodeAndAccountIdAndDateRange("VAT20", 10L, periodStart, periodEnd, TENANT_ID))
                .thenReturn(List.of(salesLine));
        when(journalLineRepository.findByTaxCodeAndAccountIdAndDateRange("VAT20", 11L, periodStart, periodEnd, TENANT_ID))
                .thenReturn(List.of(purchaseLine));

        // When
        TaxReportDto result = taxReportService.generateVatSummaryReport(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        assertEquals("VAT", result.getTaxType());
        assertEquals(periodStart, result.getPeriodStart());
        assertEquals(periodEnd, result.getPeriodEnd());
        assertEquals(new BigDecimal("200.00"), result.getTotalSalesTax());
        assertEquals(new BigDecimal("80.00"), result.getTotalPurchaseTax());
        assertEquals(new BigDecimal("120.00"), result.getNetTaxPayable());
        assertFalse(result.getLines().isEmpty());
    }

    @Test
    void generateTaxSummaryByType_returnsReportsForEachType() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        // Return empty lists for all tax types
        for (TaxType type : TaxType.values()) {
            when(taxCodeRepository.findByTaxTypeAndTenantId(type, TENANT_ID)).thenReturn(Collections.emptyList());
        }

        // When
        List<TaxReportDto> result = taxReportService.generateTaxSummaryByType(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        // Should be empty since no tax codes return any lines
        assertTrue(result.isEmpty());
    }

    @Test
    void generateVatSummaryReport_taxCodeWithNoRates_returnsZeroRate() {
        // Given
        TaxCode vatCodeNoRates = TaxCode.builder()
                .id(1L)
                .code("VAT0")
                .name("VAT Zero")
                .taxType(TaxType.VAT)
                .rates(new ArrayList<>())
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByTaxTypeAndTenantId(TaxType.VAT, TENANT_ID)).thenReturn(List.of(vatCodeNoRates));

        // When
        TaxReportDto result = taxReportService.generateVatSummaryReport(periodStart, periodEnd);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getLines().size());
        assertEquals(BigDecimal.ZERO, result.getLines().get(0).getRate());
    }
}
