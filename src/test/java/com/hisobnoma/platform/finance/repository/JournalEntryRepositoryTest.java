package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.BeforeEach;
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
class JournalEntryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private FiscalYear fiscalYear;
    private FiscalPeriod fiscalPeriod;

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

        fiscalPeriod = FiscalPeriod.builder()
                .tenantId(TENANT_ID)
                .fiscalYear(fiscalYear)
                .periodNumber(1)
                .name("January")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();
        fiscalPeriod = entityManager.persistAndFlush(fiscalPeriod);
    }

    private JournalEntry createEntry(Long tenantId, String entryNumber, LocalDate entryDate,
                                      JournalStatus status, JournalSource source,
                                      String description) {
        JournalEntry entry = JournalEntry.builder()
                .tenantId(tenantId)
                .entryNumber(entryNumber)
                .entryDate(entryDate)
                .fiscalPeriod(fiscalPeriod)
                .status(status)
                .source(source)
                .description(description)
                .build();
        return entityManager.persistAndFlush(entry);
    }

    // ---- findByEntryNumberAndTenantId ----

    @Test
    void findByEntryNumberAndTenantId_exists_returnsEntry() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Test entry");

        Optional<JournalEntry> result = journalEntryRepository.findByEntryNumberAndTenantId("JE-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("JE-001", result.get().getEntryNumber());
    }

    @Test
    void findByEntryNumberAndTenantId_wrongTenant_returnsEmpty() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Test entry");

        Optional<JournalEntry> result = journalEntryRepository.findByEntryNumberAndTenantId("JE-001", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByStatusAndTenantId ----

    @Test
    void findByStatusAndTenantId_returnsMatchingStatus() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Draft entry");
        createEntry(TENANT_ID, "JE-002", LocalDate.of(2024, 1, 16),
                JournalStatus.POSTED, JournalSource.MANUAL, "Posted entry");

        Page<JournalEntry> result = journalEntryRepository.findByStatusAndTenantId(
                TENANT_ID, JournalStatus.POSTED, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("JE-002", result.getContent().get(0).getEntryNumber());
    }

    // ---- findBySourceAndTenantId ----

    @Test
    void findBySourceAndTenantId_returnsMatchingSource() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Manual entry");
        createEntry(TENANT_ID, "JE-002", LocalDate.of(2024, 1, 16),
                JournalStatus.DRAFT, JournalSource.POS_SALE, "POS entry");

        Page<JournalEntry> result = journalEntryRepository.findBySourceAndTenantId(
                TENANT_ID, JournalSource.POS_SALE, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("JE-002", result.getContent().get(0).getEntryNumber());
    }

    // ---- findByDateRangeAndTenantId ----

    @Test
    void findByDateRangeAndTenantId_returnsEntriesInRange() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Entry 1");
        createEntry(TENANT_ID, "JE-002", LocalDate.of(2024, 1, 25),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Entry 2");
        createEntry(TENANT_ID, "JE-003", LocalDate.of(2024, 3, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Entry 3");

        List<JournalEntry> result = journalEntryRepository.findByDateRangeAndTenantId(
                TENANT_ID, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertEquals(2, result.size());
    }

    // ---- findByReferenceAndTenantId ----

    @Test
    void findByReferenceAndTenantId_returnsMatchingReference() {
        JournalEntry entry = JournalEntry.builder()
                .tenantId(TENANT_ID)
                .entryNumber("JE-001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .status(JournalStatus.POSTED)
                .source(JournalSource.ACCOUNTS_RECEIVABLE)
                .description("AR Invoice entry")
                .referenceType("AR_INVOICE")
                .referenceId(100L)
                .build();
        entityManager.persistAndFlush(entry);

        List<JournalEntry> result = journalEntryRepository.findByReferenceAndTenantId(
                TENANT_ID, "AR_INVOICE", 100L);

        assertEquals(1, result.size());
        assertEquals("JE-001", result.get(0).getEntryNumber());
    }

    // ---- searchByTenantId ----

    @Test
    void searchByTenantId_byDescription_returnsMatching() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Office supplies purchase");
        createEntry(TENANT_ID, "JE-002", LocalDate.of(2024, 1, 16),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Salary payment");

        Page<JournalEntry> result = journalEntryRepository.searchByTenantId(
                TENANT_ID, "office", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("JE-001", result.getContent().get(0).getEntryNumber());
    }

    @Test
    void searchByTenantId_byEntryNumber_returnsMatching() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Test entry");

        Page<JournalEntry> result = journalEntryRepository.searchByTenantId(
                TENANT_ID, "JE-001", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    // ---- findPendingReversalsForDate ----

    @Test
    void findPendingReversalsForDate_returnsPendingReversals() {
        JournalEntry reversing = JournalEntry.builder()
                .tenantId(TENANT_ID)
                .entryNumber("JE-001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .status(JournalStatus.POSTED)
                .source(JournalSource.MANUAL)
                .description("Reversing entry")
                .reversing(true)
                .reversalDate(LocalDate.of(2024, 2, 1))
                .build();
        entityManager.persistAndFlush(reversing);

        JournalEntry nonReversing = createEntry(TENANT_ID, "JE-002", LocalDate.of(2024, 1, 16),
                JournalStatus.POSTED, JournalSource.MANUAL, "Normal entry");

        List<JournalEntry> result = journalEntryRepository.findPendingReversalsForDate(
                TENANT_ID, LocalDate.of(2024, 2, 15));

        assertEquals(1, result.size());
        assertEquals("JE-001", result.get(0).getEntryNumber());
    }

    // ---- findMaxEntryNumberForPrefix ----

    @Test
    void findMaxEntryNumberForPrefix_returnsMax() {
        createEntry(TENANT_ID, "JE-005", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Entry 5");
        createEntry(TENANT_ID, "JE-023", LocalDate.of(2024, 1, 16),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Entry 23");

        Integer result = journalEntryRepository.findMaxEntryNumberForPrefix("JE-", TENANT_ID);

        assertEquals(23, result);
    }

    @Test
    void findMaxEntryNumberForPrefix_noEntries_returnsZero() {
        Integer result = journalEntryRepository.findMaxEntryNumberForPrefix("JE-", TENANT_ID);

        assertEquals(0, result);
    }

    // ---- existsByEntryNumberAndTenantId ----

    @Test
    void existsByEntryNumberAndTenantId_exists_returnsTrue() {
        createEntry(TENANT_ID, "JE-001", LocalDate.of(2024, 1, 15),
                JournalStatus.DRAFT, JournalSource.MANUAL, "Test entry");

        assertTrue(journalEntryRepository.existsByEntryNumberAndTenantId("JE-001", TENANT_ID));
    }

    @Test
    void existsByEntryNumberAndTenantId_notExists_returnsFalse() {
        assertFalse(journalEntryRepository.existsByEntryNumberAndTenantId("JE-999", TENANT_ID));
    }
}
