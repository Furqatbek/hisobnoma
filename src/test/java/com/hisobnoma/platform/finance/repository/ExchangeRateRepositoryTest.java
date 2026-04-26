package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ExchangeRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ExchangeRateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private ExchangeRate createExchangeRate(Long tenantId, String fromCurrency, String toCurrency,
                                             BigDecimal rate, LocalDate effectiveDate,
                                             LocalDate expiryDate, boolean active) {
        ExchangeRate exchangeRate = ExchangeRate.builder()
                .tenantId(tenantId)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .effectiveDate(effectiveDate)
                .expiryDate(expiryDate)
                .active(active)
                .build();
        return entityManager.persistAndFlush(exchangeRate);
    }

    // ---- findAllByTenantId (List) ----

    @Test
    void findAllByTenantId_includesSystemAndTenantRates() {
        createExchangeRate(null, "USD", "UZS", BigDecimal.valueOf(12500),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "EUR", "UZS", BigDecimal.valueOf(13500),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(OTHER_TENANT_ID, "GBP", "UZS", BigDecimal.valueOf(15000),
                LocalDate.of(2024, 1, 1), null, true);

        List<ExchangeRate> result = exchangeRateRepository.findAllByTenantId(TENANT_ID);

        assertEquals(2, result.size());
    }

    // ---- findByIdAndTenantId ----

    @Test
    void findByIdAndTenantId_exists_returnsRate() {
        ExchangeRate er = createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12500),
                LocalDate.of(2024, 1, 1), null, true);

        Optional<ExchangeRate> result = exchangeRateRepository.findByIdAndTenantId(er.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("USD", result.get().getFromCurrency());
    }

    // ---- findEffectiveRate ----

    @Test
    void findEffectiveRate_returnsEffectiveRate() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30), true);
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12500),
                LocalDate.of(2024, 7, 1), null, true);

        Optional<ExchangeRate> result = exchangeRateRepository.findEffectiveRate(
                TENANT_ID, "USD", "UZS", LocalDate.of(2024, 3, 15));

        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.valueOf(12000).compareTo(result.get().getRate()));
    }

    @Test
    void findEffectiveRate_expiredRate_returnsEmpty() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31), true);

        Optional<ExchangeRate> result = exchangeRateRepository.findEffectiveRate(
                TENANT_ID, "USD", "UZS", LocalDate.of(2024, 6, 1));

        assertFalse(result.isPresent());
    }

    @Test
    void findEffectiveRate_inactiveRate_returnsEmpty() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2024, 1, 1), null, false);

        Optional<ExchangeRate> result = exchangeRateRepository.findEffectiveRate(
                TENANT_ID, "USD", "UZS", LocalDate.of(2024, 3, 15));

        assertFalse(result.isPresent());
    }

    // ---- findByFromCurrencyAndToCurrency ----

    @Test
    void findByFromCurrencyAndToCurrency_returnsMatchingRates() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12500),
                LocalDate.of(2024, 7, 1), null, true);
        createExchangeRate(TENANT_ID, "EUR", "UZS", BigDecimal.valueOf(13500),
                LocalDate.of(2024, 1, 1), null, true);

        List<ExchangeRate> result = exchangeRateRepository.findByFromCurrencyAndToCurrency(
                TENANT_ID, "USD", "UZS");

        assertEquals(2, result.size());
    }

    // ---- findByEffectiveDate ----

    @Test
    void findByEffectiveDate_returnsMatchingDate() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "EUR", "UZS", BigDecimal.valueOf(13500),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "GBP", "UZS", BigDecimal.valueOf(15000),
                LocalDate.of(2024, 7, 1), null, true);

        List<ExchangeRate> result = exchangeRateRepository.findByEffectiveDate(
                TENANT_ID, LocalDate.of(2024, 1, 1));

        assertEquals(2, result.size());
    }

    // ---- findByCurrency ----

    @Test
    void findByCurrency_returnsRatesWithCurrency() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "EUR", "USD", BigDecimal.valueOf(1.1),
                LocalDate.of(2024, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "EUR", "UZS", BigDecimal.valueOf(13500),
                LocalDate.of(2024, 1, 1), null, true);

        List<ExchangeRate> result = exchangeRateRepository.findByCurrency(TENANT_ID, "USD");

        // USD appears as either fromCurrency or toCurrency
        assertTrue(result.size() >= 1);
    }

    // ---- findCurrentActiveRates ----

    @Test
    void findCurrentActiveRates_returnsActiveNonExpiredRates() {
        createExchangeRate(TENANT_ID, "USD", "UZS", BigDecimal.valueOf(12000),
                LocalDate.of(2020, 1, 1), null, true);
        createExchangeRate(TENANT_ID, "EUR", "UZS", BigDecimal.valueOf(13500),
                LocalDate.of(2020, 1, 1), null, false);

        List<ExchangeRate> result = exchangeRateRepository.findCurrentActiveRates(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("USD", result.get(0).getFromCurrency());
    }
}
