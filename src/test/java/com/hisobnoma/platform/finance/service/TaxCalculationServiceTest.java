package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.TaxCalculationRequest;
import com.hisobnoma.platform.finance.dto.TaxCalculationResult;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.entity.TaxType;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import com.hisobnoma.platform.finance.repository.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxCalculationServiceTest {

    @Mock
    private TaxCodeRepository taxCodeRepository;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private TaxCalculationService taxCalculationService;

    private TaxCode vatTaxCode;
    private TaxRate vatRate12;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        vatTaxCode = TaxCode.builder()
                .id(1L)
                .code("VAT12")
                .name("VAT 12%")
                .taxType(TaxType.VAT)
                .inclusive(false)
                .compound(false)
                .appliesToSales(true)
                .appliesToPurchases(true)
                .active(true)
                .rates(new ArrayList<>())
                .build();

        vatRate12 = TaxRate.builder()
                .id(1L)
                .taxCode(vatTaxCode)
                .name("Standard Rate")
                .rate(new BigDecimal("12.0000"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .active(true)
                .build();

        vatTaxCode.getRates().add(vatRate12);
    }

    @Test
    void shouldCalculateExclusiveTaxCorrectly() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByCodeWithRatesAndTenantId("VAT12", TENANT_ID))
                .thenReturn(Optional.of(vatTaxCode));
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("VAT12", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.of(vatRate12));

        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT12")
                .amount(new BigDecimal("1000.00"))
                .inclusive(false)
                .build();

        // When
        TaxCalculationResult result = taxCalculationService.calculateTax(request);

        // Then
        assertNotNull(result);
        assertEquals("VAT12", result.getTaxCode());
        assertEquals(new BigDecimal("12.0000"), result.getRate());
        assertEquals(new BigDecimal("1000.00"), result.getBaseAmount());
        assertEquals(0, new BigDecimal("120.0000").compareTo(result.getTaxAmount()));
        assertEquals(0, new BigDecimal("1120.0000").compareTo(result.getTotalAmount()));
        assertFalse(result.isInclusive());
    }

    @Test
    void shouldCalculateInclusiveTaxCorrectly() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByCodeWithRatesAndTenantId("VAT12", TENANT_ID))
                .thenReturn(Optional.of(vatTaxCode));
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("VAT12", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.of(vatRate12));

        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT12")
                .amount(new BigDecimal("1120.00"))
                .inclusive(true)
                .build();

        // When
        TaxCalculationResult result = taxCalculationService.calculateTax(request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("1120.00"), result.getTotalAmount());
        // Tax amount should be 1120 * 0.12 / 1.12 = 120
        assertEquals(0, new BigDecimal("120.0000").compareTo(result.getTaxAmount()));
        // Base amount should be 1120 - 120 = 1000
        assertEquals(0, new BigDecimal("1000.0000").compareTo(result.getBaseAmount()));
        assertTrue(result.isInclusive());
    }

    @Test
    void shouldReturnZeroTaxWhenNoRateFound() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByCodeWithRatesAndTenantId("VAT12", TENANT_ID))
                .thenReturn(Optional.of(vatTaxCode));
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("VAT12", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.empty());

        TaxCalculationRequest request = TaxCalculationRequest.builder()
                .taxCode("VAT12")
                .amount(new BigDecimal("1000.00"))
                .build();

        // When
        TaxCalculationResult result = taxCalculationService.calculateTax(request);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getRate());
        assertEquals(BigDecimal.ZERO, result.getTaxAmount());
        assertEquals(new BigDecimal("1000.00"), result.getTotalAmount());
    }

    @Test
    void shouldGetEffectiveRate() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("VAT12", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.of(vatRate12));

        // When
        BigDecimal rate = taxCalculationService.getEffectiveRate("VAT12", null);

        // Then
        assertEquals(new BigDecimal("12.0000"), rate);
    }

    @Test
    void shouldReturnZeroRateWhenNoEffectiveRateFound() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("INVALID", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.empty());

        // When
        BigDecimal rate = taxCalculationService.getEffectiveRate("INVALID", null);

        // Then
        assertEquals(BigDecimal.ZERO, rate);
    }

    @Test
    void shouldExtractTaxFromInclusiveAmount() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByCodeWithRatesAndTenantId("VAT12", TENANT_ID))
                .thenReturn(Optional.of(vatTaxCode));
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("VAT12", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.of(vatRate12));

        // When
        TaxCalculationResult result = taxCalculationService.extractTax("VAT12", new BigDecimal("1120.00"), null);

        // Then
        assertNotNull(result);
        assertTrue(result.isInclusive());
        assertEquals(0, new BigDecimal("120.0000").compareTo(result.getTaxAmount()));
    }

    @Test
    void shouldAddTaxToBaseAmount() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByCodeWithRatesAndTenantId("VAT12", TENANT_ID))
                .thenReturn(Optional.of(vatTaxCode));
        when(taxRateRepository.findEffectiveRateByTaxCodeAndDate("VAT12", LocalDate.now(), TENANT_ID))
                .thenReturn(Optional.of(vatRate12));

        // When
        TaxCalculationResult result = taxCalculationService.addTax("VAT12", new BigDecimal("1000.00"), null);

        // Then
        assertNotNull(result);
        assertFalse(result.isInclusive());
        assertEquals(new BigDecimal("1000.00"), result.getBaseAmount());
        assertEquals(0, new BigDecimal("1120.0000").compareTo(result.getTotalAmount()));
    }
}
