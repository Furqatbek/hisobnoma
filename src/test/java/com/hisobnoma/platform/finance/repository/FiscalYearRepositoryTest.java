package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class FiscalYearRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private FiscalYear createFiscalYear(Long tenantId, int year, String name,
                                         LocalDate startDate, LocalDate endDate,
                                         boolean current, boolean closed) {
        FiscalYear fy = FiscalYear.builder()
                .tenantId(tenantId)
                .year(year)
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .current(current)
                .closed(closed)
                .build();
        return entityManager.persistAndFlush(fy);
    }

    // ---- findAllByTenantId (List) ----

    @Test
    void findAllByTenantId_returnsOrderedByYearDesc() {
        createFiscalYear(TENANT_ID, 2023, "FY 2023",
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), false, true);
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        List<FiscalYear> result = fiscalYearRepository.findAllByTenantId(TENANT_ID);

        assertEquals(2, result.size());
        assertEquals(2024, result.get(0).getYear());
        assertEquals(2023, result.get(1).getYear());
    }

    @Test
    void findAllByTenantId_otherTenant_excluded() {
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);
        createFiscalYear(OTHER_TENANT_ID, 2024, "Other FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        List<FiscalYear> result = fiscalYearRepository.findAllByTenantId(TENANT_ID);

        assertEquals(1, result.size());
    }

    // ---- findByIdAndTenantId ----

    @Test
    void findByIdAndTenantId_exists_returnsYear() {
        FiscalYear fy = createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        Optional<FiscalYear> result = fiscalYearRepository.findByIdAndTenantId(fy.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("FY 2024", result.get().getName());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        FiscalYear fy = createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        Optional<FiscalYear> result = fiscalYearRepository.findByIdAndTenantId(fy.getId(), OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByYearAndTenantId ----

    @Test
    void findByYearAndTenantId_exists_returnsYear() {
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        Optional<FiscalYear> result = fiscalYearRepository.findByYearAndTenantId(2024, TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("FY 2024", result.get().getName());
    }

    @Test
    void findByYearAndTenantId_notExists_returnsEmpty() {
        Optional<FiscalYear> result = fiscalYearRepository.findByYearAndTenantId(2099, TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findCurrentByTenantId ----

    @Test
    void findCurrentByTenantId_returnsCurrent() {
        createFiscalYear(TENANT_ID, 2023, "FY 2023",
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), false, true);
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        Optional<FiscalYear> result = fiscalYearRepository.findCurrentByTenantId(TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getYear());
    }

    @Test
    void findCurrentByTenantId_noCurrent_returnsEmpty() {
        createFiscalYear(TENANT_ID, 2023, "FY 2023",
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), false, true);

        Optional<FiscalYear> result = fiscalYearRepository.findCurrentByTenantId(TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByDateAndTenantId ----

    @Test
    void findByDateAndTenantId_dateInYear_returnsYear() {
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        Optional<FiscalYear> result = fiscalYearRepository.findByDateAndTenantId(
                LocalDate.of(2024, 6, 15), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals(2024, result.get().getYear());
    }

    @Test
    void findByDateAndTenantId_dateOutOfYear_returnsEmpty() {
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        Optional<FiscalYear> result = fiscalYearRepository.findByDateAndTenantId(
                LocalDate.of(2025, 6, 15), TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findOpenYearsByTenantId ----

    @Test
    void findOpenYearsByTenantId_returnsOpenOnly() {
        createFiscalYear(TENANT_ID, 2023, "FY 2023",
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), false, true);
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        List<FiscalYear> result = fiscalYearRepository.findOpenYearsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals(2024, result.get(0).getYear());
    }

    // ---- existsByYearAndTenantId ----

    @Test
    void existsByYearAndTenantId_exists_returnsTrue() {
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        assertTrue(fiscalYearRepository.existsByYearAndTenantId(2024, TENANT_ID));
    }

    @Test
    void existsByYearAndTenantId_notExists_returnsFalse() {
        assertFalse(fiscalYearRepository.existsByYearAndTenantId(2099, TENANT_ID));
    }

    @Test
    void existsByYearAndTenantId_wrongTenant_returnsFalse() {
        createFiscalYear(TENANT_ID, 2024, "FY 2024",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), true, false);

        assertFalse(fiscalYearRepository.existsByYearAndTenantId(2024, OTHER_TENANT_ID));
    }
}
