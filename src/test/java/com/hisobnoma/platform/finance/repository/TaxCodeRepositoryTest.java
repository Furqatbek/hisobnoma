package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class TaxCodeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaxCodeRepository taxCodeRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private TaxCode createTaxCode(Long tenantId, String code, String name,
                                   TaxType taxType, boolean active,
                                   boolean appliesToSales, boolean appliesToPurchases,
                                   int sortOrder) {
        TaxCode taxCode = TaxCode.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .taxType(taxType)
                .active(active)
                .appliesToSales(appliesToSales)
                .appliesToPurchases(appliesToPurchases)
                .sortOrder(sortOrder)
                .build();
        return entityManager.persistAndFlush(taxCode);
    }

    // ---- findAllByTenantId (List) ----

    @Test
    void findAllByTenantId_includesSystemAndTenantCodes() {
        createTaxCode(null, "VAT-STD", "Standard VAT", TaxType.VAT,
                true, true, true, 1);
        createTaxCode(TENANT_ID, "VAT-RED", "Reduced VAT", TaxType.VAT,
                true, true, true, 2);
        createTaxCode(OTHER_TENANT_ID, "GST", "Other Tenant GST", TaxType.GST,
                true, true, true, 3);

        List<TaxCode> result = taxCodeRepository.findAllByTenantId(TENANT_ID);

        assertEquals(2, result.size());
    }

    // ---- findByCodeAndTenantId ----

    @Test
    void findByCodeAndTenantId_tenantCode_returnsCode() {
        createTaxCode(TENANT_ID, "VAT-10", "VAT 10%", TaxType.VAT,
                true, true, true, 1);

        Optional<TaxCode> result = taxCodeRepository.findByCodeAndTenantId("VAT-10", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("VAT 10%", result.get().getName());
    }

    @Test
    void findByCodeAndTenantId_systemCode_returnsCode() {
        createTaxCode(null, "VAT-STD", "Standard VAT", TaxType.VAT,
                true, true, true, 1);

        Optional<TaxCode> result = taxCodeRepository.findByCodeAndTenantId("VAT-STD", TENANT_ID);

        assertTrue(result.isPresent());
    }

    // ---- findAllActiveByTenantId ----

    @Test
    void findAllActiveByTenantId_returnsActiveOnly() {
        createTaxCode(TENANT_ID, "VAT-10", "Active VAT", TaxType.VAT,
                true, true, true, 1);
        createTaxCode(TENANT_ID, "VAT-OLD", "Inactive VAT", TaxType.VAT,
                false, true, true, 2);

        List<TaxCode> result = taxCodeRepository.findAllActiveByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Active VAT", result.get(0).getName());
    }

    // ---- findByTaxTypeAndTenantId ----

    @Test
    void findByTaxTypeAndTenantId_returnsMatchingType() {
        createTaxCode(TENANT_ID, "VAT-10", "VAT 10%", TaxType.VAT,
                true, true, true, 1);
        createTaxCode(TENANT_ID, "GST-5", "GST 5%", TaxType.GST,
                true, true, true, 2);

        List<TaxCode> result = taxCodeRepository.findByTaxTypeAndTenantId(TaxType.VAT, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("VAT 10%", result.get(0).getName());
    }

    // ---- findSalesTaxCodesByTenantId ----

    @Test
    void findSalesTaxCodesByTenantId_returnsSalesApplicable() {
        createTaxCode(TENANT_ID, "VAT-S", "Sales VAT", TaxType.VAT,
                true, true, false, 1);
        createTaxCode(TENANT_ID, "VAT-P", "Purchase VAT", TaxType.VAT,
                true, false, true, 2);

        List<TaxCode> result = taxCodeRepository.findSalesTaxCodesByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Sales VAT", result.get(0).getName());
    }

    // ---- findPurchaseTaxCodesByTenantId ----

    @Test
    void findPurchaseTaxCodesByTenantId_returnsPurchaseApplicable() {
        createTaxCode(TENANT_ID, "VAT-S", "Sales VAT", TaxType.VAT,
                true, true, false, 1);
        createTaxCode(TENANT_ID, "VAT-P", "Purchase VAT", TaxType.VAT,
                true, false, true, 2);

        List<TaxCode> result = taxCodeRepository.findPurchaseTaxCodesByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Purchase VAT", result.get(0).getName());
    }

    // ---- existsByCodeAndTenantId ----

    @Test
    void existsByCodeAndTenantId_exists_returnsTrue() {
        createTaxCode(TENANT_ID, "VAT-10", "VAT 10%", TaxType.VAT,
                true, true, true, 1);

        assertTrue(taxCodeRepository.existsByCodeAndTenantId("VAT-10", TENANT_ID));
    }

    @Test
    void existsByCodeAndTenantId_notExists_returnsFalse() {
        assertFalse(taxCodeRepository.existsByCodeAndTenantId("NONEXISTENT", TENANT_ID));
    }

    // ---- existsByCodeAndTenantIdIsNull ----

    @Test
    void existsByCodeAndTenantIdIsNull_systemCode_returnsTrue() {
        createTaxCode(null, "VAT-STD", "Standard VAT", TaxType.VAT,
                true, true, true, 1);

        assertTrue(taxCodeRepository.existsByCodeAndTenantIdIsNull("VAT-STD"));
    }

    @Test
    void existsByCodeAndTenantIdIsNull_tenantCode_returnsFalse() {
        createTaxCode(TENANT_ID, "VAT-10", "VAT 10%", TaxType.VAT,
                true, true, true, 1);

        assertFalse(taxCodeRepository.existsByCodeAndTenantIdIsNull("VAT-10"));
    }

    // ---- searchByTenantId ----

    @Test
    void searchByTenantId_byCode_returnsMatching() {
        createTaxCode(TENANT_ID, "VAT-10", "VAT 10%", TaxType.VAT,
                true, true, true, 1);
        createTaxCode(TENANT_ID, "GST-5", "GST 5%", TaxType.GST,
                true, true, true, 2);

        Page<TaxCode> result = taxCodeRepository.searchByTenantId(
                "VAT", TENANT_ID, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchByTenantId_byName_returnsMatching() {
        createTaxCode(TENANT_ID, "TC-01", "Standard Rate", TaxType.VAT,
                true, true, true, 1);
        createTaxCode(TENANT_ID, "TC-02", "Zero Rate", TaxType.VAT,
                true, true, true, 2);

        Page<TaxCode> result = taxCodeRepository.searchByTenantId(
                "standard", TENANT_ID, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }
}
