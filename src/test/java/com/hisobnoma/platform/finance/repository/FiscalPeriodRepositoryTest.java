package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import org.junit.jupiter.api.BeforeEach;
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
class FiscalPeriodRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private FiscalYear fiscalYear;

    @BeforeEach
    void setUp() {
        fiscalYear = FiscalYear.builder()
                .tenantId(TENANT_ID)
                .year(2024)
                .name("FY 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();
        fiscalYear = entityManager.persistAndFlush(fiscalYear);
    }

    private FiscalPeriod createPeriod(Long tenantId, FiscalYear year, int periodNumber,
                                       String name, LocalDate startDate, LocalDate endDate,
                                       PeriodStatus status) {
        FiscalPeriod period = FiscalPeriod.builder()
                .tenantId(tenantId)
                .fiscalYear(year)
                .periodNumber(periodNumber)
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .build();
        return entityManager.persistAndFlush(period);
    }

    // ---- findByIdAndTenantId ----

    @Test
    void findByIdAndTenantId_exists_returnsPeriod() {
        FiscalPeriod period = createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.OPEN);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findByIdAndTenantId(period.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("January", result.get().getName());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        FiscalPeriod period = createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.OPEN);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findByIdAndTenantId(period.getId(), OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByFiscalYearId ----

    @Test
    void findByFiscalYearId_returnsPeriods() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.OPEN);
        createPeriod(TENANT_ID, fiscalYear, 2, "February",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), PeriodStatus.OPEN);

        List<FiscalPeriod> result = fiscalPeriodRepository.findByFiscalYearId(fiscalYear.getId());

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getPeriodNumber());
        assertEquals(2, result.get(1).getPeriodNumber());
    }

    // ---- findByDateAndTenantId ----

    @Test
    void findByDateAndTenantId_dateInPeriod_returnsPeriod() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.OPEN);
        createPeriod(TENANT_ID, fiscalYear, 2, "February",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), PeriodStatus.OPEN);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findByDateAndTenantId(
                LocalDate.of(2024, 1, 15), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("January", result.get().getName());
    }

    @Test
    void findByDateAndTenantId_dateNotInAnyPeriod_returnsEmpty() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.OPEN);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findByDateAndTenantId(
                LocalDate.of(2024, 6, 15), TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByStatusAndTenantId ----

    @Test
    void findByStatusAndTenantId_returnsMatchingStatus() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.CLOSED);
        createPeriod(TENANT_ID, fiscalYear, 2, "February",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), PeriodStatus.OPEN);
        createPeriod(TENANT_ID, fiscalYear, 3, "March",
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), PeriodStatus.OPEN);

        List<FiscalPeriod> result = fiscalPeriodRepository.findByStatusAndTenantId(TENANT_ID, PeriodStatus.OPEN);

        assertEquals(2, result.size());
    }

    // ---- findOpenPeriodsByTenantId ----

    @Test
    void findOpenPeriodsByTenantId_returnsOpenPeriods() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.CLOSED);
        createPeriod(TENANT_ID, fiscalYear, 2, "February",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), PeriodStatus.OPEN);

        List<FiscalPeriod> result = fiscalPeriodRepository.findOpenPeriodsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("February", result.get(0).getName());
    }

    // ---- findFirstOpenPeriodByTenantId ----

    @Test
    void findFirstOpenPeriodByTenantId_returnsFirstOpen() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.CLOSED);
        createPeriod(TENANT_ID, fiscalYear, 2, "February",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), PeriodStatus.OPEN);
        createPeriod(TENANT_ID, fiscalYear, 3, "March",
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), PeriodStatus.OPEN);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findFirstOpenPeriodByTenantId(TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("February", result.get().getName());
    }

    @Test
    void findFirstOpenPeriodByTenantId_noOpenPeriods_returnsEmpty() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.CLOSED);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findFirstOpenPeriodByTenantId(TENANT_ID);

        // The query doesn't use LIMIT, so it may return empty or the first closed period
        // but the intent is to find the first open one; since there's none, we expect the Optional to contain
        // a period or be empty depending on query results. In this case, no OPEN periods exist.
        // Since there are no OPEN periods, the list is empty, and the Optional is empty.
        assertFalse(result.isPresent());
    }

    // ---- findByFiscalYearAndPeriodNumber ----

    @Test
    void findByFiscalYearAndPeriodNumber_exists_returnsPeriod() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.OPEN);

        Optional<FiscalPeriod> result = fiscalPeriodRepository.findByFiscalYearAndPeriodNumber(
                fiscalYear.getId(), 1);

        assertTrue(result.isPresent());
        assertEquals("January", result.get().getName());
    }

    @Test
    void findByFiscalYearAndPeriodNumber_notExists_returnsEmpty() {
        Optional<FiscalPeriod> result = fiscalPeriodRepository.findByFiscalYearAndPeriodNumber(
                fiscalYear.getId(), 99);

        assertFalse(result.isPresent());
    }

    // ---- countOpenPeriodsByFiscalYear ----

    @Test
    void countOpenPeriodsByFiscalYear_returnsCorrectCount() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.CLOSED);
        createPeriod(TENANT_ID, fiscalYear, 2, "February",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), PeriodStatus.OPEN);
        createPeriod(TENANT_ID, fiscalYear, 3, "March",
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), PeriodStatus.OPEN);

        long result = fiscalPeriodRepository.countOpenPeriodsByFiscalYear(fiscalYear.getId());

        assertEquals(2, result);
    }

    @Test
    void countOpenPeriodsByFiscalYear_noOpenPeriods_returnsZero() {
        createPeriod(TENANT_ID, fiscalYear, 1, "January",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), PeriodStatus.CLOSED);

        long result = fiscalPeriodRepository.countOpenPeriodsByFiscalYear(fiscalYear.getId());

        assertEquals(0, result);
    }
}
