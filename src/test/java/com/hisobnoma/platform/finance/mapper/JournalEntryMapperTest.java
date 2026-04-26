package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JournalEntryMapperTest {

    @Autowired
    private JournalEntryMapper journalEntryMapper;

    private JournalEntry buildTestEntry() {
        FiscalPeriod fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .name("January 2024")
                .periodNumber(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();

        Account cashAccount = Account.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .childAccounts(new ArrayList<>())
                .build();

        Account revenueAccount = Account.builder()
                .id(2L)
                .code("4100")
                .name("Sales Revenue")
                .accountType(AccountType.REVENUE)
                .normalBalance(NormalBalance.CREDIT)
                .childAccounts(new ArrayList<>())
                .build();

        JournalEntry entry = JournalEntry.builder()
                .id(1L)
                .tenantId(1L)
                .entryNumber("JE-202401-0001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .description("Test journal entry")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .referenceType("MANUAL")
                .referenceNumber("REF-001")
                .totalDebit(new BigDecimal("1000.00"))
                .totalCredit(new BigDecimal("1000.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .recurring(false)
                .reversing(false)
                .postedAt(Instant.now())
                .postedBy(1L)
                .notes("Test notes")
                .internalNotes("Internal notes")
                .lines(new ArrayList<>())
                .build();

        JournalLine debitLine = JournalLine.builder()
                .id(1L)
                .tenantId(1L)
                .journalEntry(entry)
                .lineNumber(1)
                .account(cashAccount)
                .description("Cash debit")
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .baseDebitAmount(new BigDecimal("1000.00"))
                .baseCreditAmount(BigDecimal.ZERO)
                .build();

        JournalLine creditLine = JournalLine.builder()
                .id(2L)
                .tenantId(1L)
                .journalEntry(entry)
                .lineNumber(2)
                .account(revenueAccount)
                .description("Revenue credit")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("1000.00"))
                .baseDebitAmount(BigDecimal.ZERO)
                .baseCreditAmount(new BigDecimal("1000.00"))
                .build();

        entry.getLines().add(debitLine);
        entry.getLines().add(creditLine);

        return entry;
    }

    @Test
    void toDto_mapsAllFieldsIncludingLines() {
        // Given
        JournalEntry entry = buildTestEntry();

        // When
        JournalEntryDto dto = journalEntryMapper.toDto(entry);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("JE-202401-0001", dto.getEntryNumber());
        assertEquals(LocalDate.of(2024, 1, 15), dto.getEntryDate());
        assertEquals(1L, dto.getFiscalPeriodId());
        assertEquals("January 2024", dto.getFiscalPeriodName());
        assertEquals("Test journal entry", dto.getDescription());
        assertEquals(JournalStatus.DRAFT, dto.getStatus());
        assertEquals(JournalSource.MANUAL, dto.getSource());
        assertEquals("MANUAL", dto.getReferenceType());
        assertEquals("REF-001", dto.getReferenceNumber());
        assertEquals(new BigDecimal("1000.00"), dto.getTotalDebit());
        assertEquals(new BigDecimal("1000.00"), dto.getTotalCredit());
        assertEquals("UZS", dto.getCurrency());
        assertEquals(BigDecimal.ONE, dto.getExchangeRate());
        assertTrue(dto.isBalanced());
        assertEquals("Test notes", dto.getNotes());
        assertEquals("Internal notes", dto.getInternalNotes());

        // Lines should be mapped
        assertNotNull(dto.getLines());
        assertEquals(2, dto.getLines().size());
        assertEquals("1110", dto.getLines().get(0).getAccountCode());
        assertEquals("4100", dto.getLines().get(1).getAccountCode());
    }

    @Test
    void toDtoWithoutLines_mapsFieldsButIgnoresLines() {
        // Given
        JournalEntry entry = buildTestEntry();

        // When
        JournalEntryDto dto = journalEntryMapper.toDtoWithoutLines(entry);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("JE-202401-0001", dto.getEntryNumber());
        assertEquals(LocalDate.of(2024, 1, 15), dto.getEntryDate());
        assertEquals(1L, dto.getFiscalPeriodId());
        assertEquals("January 2024", dto.getFiscalPeriodName());
        assertEquals("Test journal entry", dto.getDescription());
        assertEquals(JournalStatus.DRAFT, dto.getStatus());
        assertEquals(JournalSource.MANUAL, dto.getSource());
        assertTrue(dto.isBalanced());

        // Lines should be null (ignored)
        assertNull(dto.getLines());
    }

    @Test
    void toDto_unbalancedEntry_returnsBalancedFalse() {
        // Given
        FiscalPeriod fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .name("January 2024")
                .periodNumber(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();

        Account account = Account.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .childAccounts(new ArrayList<>())
                .build();

        JournalEntry entry = JournalEntry.builder()
                .id(1L)
                .entryNumber("JE-TEST")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .description("Unbalanced")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .totalDebit(new BigDecimal("1000.00"))
                .totalCredit(new BigDecimal("500.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .lines(new ArrayList<>())
                .build();

        JournalLine line = JournalLine.builder()
                .id(1L)
                .journalEntry(entry)
                .lineNumber(1)
                .account(account)
                .debitAmount(new BigDecimal("1000.00"))
                .creditAmount(BigDecimal.ZERO)
                .baseDebitAmount(new BigDecimal("1000.00"))
                .baseCreditAmount(BigDecimal.ZERO)
                .build();

        entry.getLines().add(line);

        // When
        JournalEntryDto dto = journalEntryMapper.toDto(entry);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isBalanced());
    }
}
