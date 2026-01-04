package com.hisobnoma.platform.finance.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TaxRateTest {

    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
        taxRate = TaxRate.builder()
                .id(1L)
                .name("Standard VAT")
                .rate(new BigDecimal("12.0000"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .effectiveTo(null)
                .active(true)
                .build();
    }

    @Test
    void shouldCalculateExclusiveTaxCorrectly() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");

        // When
        BigDecimal tax = taxRate.calculateTax(amount, false);

        // Then
        assertEquals(0, new BigDecimal("120.0000").compareTo(tax));
    }

    @Test
    void shouldCalculateInclusiveTaxCorrectly() {
        // Given - amount includes tax (12% tax = 120, so total is 1120)
        BigDecimal inclusiveAmount = new BigDecimal("1120.00");

        // When
        BigDecimal tax = taxRate.calculateTax(inclusiveAmount, true);

        // Then - tax = 1120 * 0.12 / 1.12 = 120
        assertEquals(0, new BigDecimal("120.0000").compareTo(tax));
    }

    @Test
    void shouldCalculateBaseAmountFromInclusive() {
        // Given
        BigDecimal inclusiveAmount = new BigDecimal("1120.00");

        // When
        BigDecimal baseAmount = taxRate.calculateBaseAmount(inclusiveAmount);

        // Then - base = 1120 / 1.12 = 1000
        assertEquals(0, new BigDecimal("1000.0000").compareTo(baseAmount));
    }

    @Test
    void shouldCalculateTotalAmountFromBase() {
        // Given
        BigDecimal baseAmount = new BigDecimal("1000.00");

        // When
        BigDecimal totalAmount = taxRate.calculateTotalAmount(baseAmount);

        // Then - total = 1000 + 120 = 1120
        assertEquals(0, new BigDecimal("1120.0000").compareTo(totalAmount));
    }

    @Test
    void shouldReturnZeroTaxForNullAmount() {
        // When
        BigDecimal tax = taxRate.calculateTax(null, false);

        // Then
        assertEquals(BigDecimal.ZERO, tax);
    }

    @Test
    void shouldReturnZeroTaxForZeroRate() {
        // Given
        taxRate.setRate(BigDecimal.ZERO);
        BigDecimal amount = new BigDecimal("1000.00");

        // When
        BigDecimal tax = taxRate.calculateTax(amount, false);

        // Then
        assertEquals(BigDecimal.ZERO, tax);
    }

    @Test
    void shouldBeEffectiveForDateWithinRange() {
        // Given
        LocalDate testDate = LocalDate.of(2023, 6, 15);

        // When
        boolean isEffective = taxRate.isEffectiveFor(testDate);

        // Then
        assertTrue(isEffective);
    }

    @Test
    void shouldNotBeEffectiveForDateBeforeStart() {
        // Given
        LocalDate testDate = LocalDate.of(2019, 12, 31);

        // When
        boolean isEffective = taxRate.isEffectiveFor(testDate);

        // Then
        assertFalse(isEffective);
    }

    @Test
    void shouldNotBeEffectiveForDateAfterEnd() {
        // Given
        taxRate.setEffectiveTo(LocalDate.of(2022, 12, 31));
        LocalDate testDate = LocalDate.of(2023, 1, 1);

        // When
        boolean isEffective = taxRate.isEffectiveFor(testDate);

        // Then
        assertFalse(isEffective);
    }

    @Test
    void shouldNotBeEffectiveWhenInactive() {
        // Given
        taxRate.setActive(false);
        LocalDate testDate = LocalDate.of(2023, 6, 15);

        // When
        boolean isEffective = taxRate.isEffectiveFor(testDate);

        // Then
        assertFalse(isEffective);
    }

    @Test
    void shouldRespectMinimumAmount() {
        // Given
        taxRate.setMinAmount(new BigDecimal("500.00"));
        BigDecimal amount = new BigDecimal("400.00"); // Below minimum

        // When
        BigDecimal tax = taxRate.calculateTax(amount, false);

        // Then
        assertEquals(BigDecimal.ZERO, tax);
    }

    @Test
    void shouldCapAtMaximumAmount() {
        // Given
        taxRate.setMaxAmount(new BigDecimal("1000.00"));
        BigDecimal amount = new BigDecimal("2000.00"); // Above maximum

        // When
        BigDecimal tax = taxRate.calculateTax(amount, false);

        // Then - should calculate on max amount only (1000 * 0.12 = 120)
        assertEquals(0, new BigDecimal("120.0000").compareTo(tax));
    }

    @Test
    void shouldHandleSmallAmounts() {
        // Given
        BigDecimal amount = new BigDecimal("0.50");

        // When
        BigDecimal tax = taxRate.calculateTax(amount, false);

        // Then - 0.50 * 0.12 = 0.06
        assertEquals(0, new BigDecimal("0.0600").compareTo(tax));
    }

    @Test
    void shouldHandleLargeAmounts() {
        // Given
        BigDecimal amount = new BigDecimal("1000000000.00");

        // When
        BigDecimal tax = taxRate.calculateTax(amount, false);

        // Then
        assertEquals(0, new BigDecimal("120000000.0000").compareTo(tax));
    }
}
