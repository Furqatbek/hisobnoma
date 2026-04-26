package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class CurrencyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CurrencyRepository currencyRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private Currency createCurrency(Long tenantId, String code, String name, String symbol,
                                     boolean active, boolean baseCurrency, int sortOrder) {
        Currency currency = Currency.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .symbol(symbol)
                .active(active)
                .baseCurrency(baseCurrency)
                .sortOrder(sortOrder)
                .build();
        return entityManager.persistAndFlush(currency);
    }

    // ---- findAllByTenantId (List) ----

    @Test
    void findAllByTenantId_includesSystemAndTenantCurrencies() {
        createCurrency(null, "USD", "US Dollar", "$", true, false, 1);
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 2);
        createCurrency(OTHER_TENANT_ID, "EUR", "Euro", "€", true, false, 3);

        List<Currency> result = currencyRepository.findAllByTenantId(TENANT_ID);

        assertEquals(2, result.size());
    }

    // ---- findByCodeAndTenantId ----

    @Test
    void findByCodeAndTenantId_tenantCurrency_returnsCurrency() {
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 1);

        Optional<Currency> result = currencyRepository.findByCodeAndTenantId("UZS", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Uzbekistani Som", result.get().getName());
    }

    @Test
    void findByCodeAndTenantId_systemCurrency_returnsCurrency() {
        createCurrency(null, "USD", "US Dollar", "$", true, false, 1);

        Optional<Currency> result = currencyRepository.findByCodeAndTenantId("USD", TENANT_ID);

        assertTrue(result.isPresent());
    }

    @Test
    void findByCodeAndTenantId_notExists_returnsEmpty() {
        Optional<Currency> result = currencyRepository.findByCodeAndTenantId("GBP", TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByCode ----

    @Test
    void findByCode_systemCurrency_returnsCurrency() {
        createCurrency(null, "USD", "US Dollar", "$", true, false, 1);

        Optional<Currency> result = currencyRepository.findByCode("USD");

        assertTrue(result.isPresent());
    }

    @Test
    void findByCode_tenantCurrency_returnsEmpty() {
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 1);

        Optional<Currency> result = currencyRepository.findByCode("UZS");

        assertFalse(result.isPresent());
    }

    // ---- findAllActiveByTenantId ----

    @Test
    void findAllActiveByTenantId_returnsActiveOnly() {
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 1);
        createCurrency(TENANT_ID, "OLD", "Old Currency", "X", false, false, 2);

        List<Currency> result = currencyRepository.findAllActiveByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("UZS", result.get(0).getCode());
    }

    // ---- findBaseCurrencyByTenantId ----

    @Test
    void findBaseCurrencyByTenantId_returnsBaseCurrency() {
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 1);
        createCurrency(TENANT_ID, "USD", "US Dollar", "$", true, false, 2);

        Optional<Currency> result = currencyRepository.findBaseCurrencyByTenantId(TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("UZS", result.get().getCode());
    }

    @Test
    void findBaseCurrencyByTenantId_noBaseCurrency_returnsEmpty() {
        createCurrency(TENANT_ID, "USD", "US Dollar", "$", true, false, 1);

        Optional<Currency> result = currencyRepository.findBaseCurrencyByTenantId(TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- existsByCodeAndTenantId ----

    @Test
    void existsByCodeAndTenantId_exists_returnsTrue() {
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 1);

        assertTrue(currencyRepository.existsByCodeAndTenantId("UZS", TENANT_ID));
    }

    @Test
    void existsByCodeAndTenantId_notExists_returnsFalse() {
        assertFalse(currencyRepository.existsByCodeAndTenantId("GBP", TENANT_ID));
    }

    // ---- existsByCodeAndTenantIdIsNull ----

    @Test
    void existsByCodeAndTenantIdIsNull_systemCurrency_returnsTrue() {
        createCurrency(null, "USD", "US Dollar", "$", true, false, 1);

        assertTrue(currencyRepository.existsByCodeAndTenantIdIsNull("USD"));
    }

    @Test
    void existsByCodeAndTenantIdIsNull_tenantCurrency_returnsFalse() {
        createCurrency(TENANT_ID, "UZS", "Uzbekistani Som", "so'm", true, true, 1);

        assertFalse(currencyRepository.existsByCodeAndTenantIdIsNull("UZS"));
    }
}
