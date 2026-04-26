package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.enums.PriceListType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class PriceListRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PriceListRepository priceListRepository;

    private static final Long TENANT_ID = 1L;

    private PriceList persistPriceList(String code, String name, PriceListType type,
                                        boolean active, boolean isDefault,
                                        LocalDate startDate, LocalDate endDate,
                                        int priority, Long locationId) {
        PriceList pl = PriceList.builder()
                .code(code).name(name).type(type)
                .active(active).defaultPriceList(isDefault)
                .startDate(startDate).endDate(endDate)
                .priority(priority).locationId(locationId)
                .build();
        pl.setTenantId(TENANT_ID);
        return em.persistAndFlush(pl);
    }

    @Test
    void findByIdAndTenantId_existing_returnsPriceList() {
        PriceList pl = persistPriceList("PL1", "Standard", PriceListType.STANDARD, true, false, null, null, 0, null);

        Optional<PriceList> result = priceListRepository.findByIdAndTenantId(pl.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("PL1", result.get().getCode());
    }

    @Test
    void findByCodeAndTenantId_existing_returnsPriceList() {
        persistPriceList("PL-FIND", "Find Me", PriceListType.STANDARD, true, false, null, null, 0, null);

        Optional<PriceList> result = priceListRepository.findByCodeAndTenantId("PL-FIND", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Find Me", result.get().getName());
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistPriceList("PL1", "List 1", PriceListType.STANDARD, true, false, null, null, 0, null);
        persistPriceList("PL2", "List 2", PriceListType.WHOLESALE, true, false, null, null, 0, null);

        Page<PriceList> result = priceListRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchByTenantId_matchesByCodeOrName() {
        persistPriceList("VIP-PL", "VIP Pricing", PriceListType.VIP, true, false, null, null, 0, null);
        persistPriceList("STD-PL", "Standard Pricing", PriceListType.STANDARD, true, false, null, null, 0, null);

        Page<PriceList> result = priceListRepository.searchByTenantId("VIP", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndActive_filtersCorrectly() {
        persistPriceList("PL1", "Active", PriceListType.STANDARD, true, false, null, null, 0, null);
        persistPriceList("PL2", "Inactive", PriceListType.STANDARD, false, false, null, null, 0, null);

        List<PriceList> result = priceListRepository.findByTenantIdAndActive(TENANT_ID, true);

        assertEquals(1, result.size());
        assertEquals("Active", result.get(0).getName());
    }

    @Test
    void findByTenantIdAndType_filtersCorrectly() {
        persistPriceList("PL1", "Standard", PriceListType.STANDARD, true, false, null, null, 0, null);
        persistPriceList("PL2", "Wholesale", PriceListType.WHOLESALE, true, false, null, null, 0, null);

        List<PriceList> result = priceListRepository.findByTenantIdAndType(TENANT_ID, PriceListType.WHOLESALE);

        assertEquals(1, result.size());
        assertEquals("Wholesale", result.get(0).getName());
    }

    @Test
    void findByTenantIdAndDefaultPriceListTrue_returnsDefault() {
        persistPriceList("PL1", "Standard", PriceListType.STANDARD, true, true, null, null, 0, null);
        persistPriceList("PL2", "Wholesale", PriceListType.WHOLESALE, true, false, null, null, 0, null);

        Optional<PriceList> result = priceListRepository.findByTenantIdAndDefaultPriceListTrue(TENANT_ID);

        assertTrue(result.isPresent());
        assertTrue(result.get().isDefaultPriceList());
    }

    @Test
    void findEffectivePriceLists_filtersDateAndActive() {
        LocalDate today = LocalDate.now();
        persistPriceList("PL1", "Current", PriceListType.STANDARD, true, false,
                today.minusDays(10), today.plusDays(10), 1, null);
        persistPriceList("PL2", "Expired", PriceListType.STANDARD, true, false,
                today.minusDays(30), today.minusDays(1), 0, null);
        persistPriceList("PL3", "Inactive", PriceListType.STANDARD, false, false, null, null, 0, null);

        List<PriceList> result = priceListRepository.findEffectivePriceLists(TENANT_ID, today);

        assertEquals(1, result.size());
        assertEquals("Current", result.get(0).getName());
    }

    @Test
    void findEffectivePriceListsForLocation_filtersLocationAndDate() {
        LocalDate today = LocalDate.now();
        persistPriceList("PL1", "Global", PriceListType.STANDARD, true, false, null, null, 1, null);
        persistPriceList("PL2", "Location Specific", PriceListType.STANDARD, true, false, null, null, 2, 100L);
        persistPriceList("PL3", "Other Location", PriceListType.STANDARD, true, false, null, null, 0, 200L);

        List<PriceList> result = priceListRepository.findEffectivePriceListsForLocation(TENANT_ID, 100L, today);

        assertEquals(2, result.size());
    }

    @Test
    void existsByCodeAndTenantId_existing_returnsTrue() {
        persistPriceList("PL-EXISTS", "Exists", PriceListType.STANDARD, true, false, null, null, 0, null);

        assertTrue(priceListRepository.existsByCodeAndTenantId("PL-EXISTS", TENANT_ID));
        assertFalse(priceListRepository.existsByCodeAndTenantId("PL-NOPE", TENANT_ID));
    }

    @Test
    void countActiveByTenantId_returnsCount() {
        persistPriceList("PL1", "Active 1", PriceListType.STANDARD, true, false, null, null, 0, null);
        persistPriceList("PL2", "Active 2", PriceListType.WHOLESALE, true, false, null, null, 0, null);
        persistPriceList("PL3", "Inactive", PriceListType.STANDARD, false, false, null, null, 0, null);

        long result = priceListRepository.countActiveByTenantId(TENANT_ID);

        assertEquals(2L, result);
    }
}
