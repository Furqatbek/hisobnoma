package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.JournalLineDto;
import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JournalLineMapperTest {

    @Autowired
    private JournalLineMapper journalLineMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        FiscalPeriod fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .name("January 2024")
                .periodNumber(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();

        JournalEntry entry = JournalEntry.builder()
                .id(100L)
                .tenantId(1L)
                .entryNumber("JE-202401-0001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .description("Test entry")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .lines(new ArrayList<>())
                .build();

        Account account = Account.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .childAccounts(new ArrayList<>())
                .build();

        JournalLine line = JournalLine.builder()
                .id(1L)
                .tenantId(1L)
                .journalEntry(entry)
                .lineNumber(1)
                .account(account)
                .description("Cash debit line")
                .debitAmount(new BigDecimal("5000.00"))
                .creditAmount(BigDecimal.ZERO)
                .baseDebitAmount(new BigDecimal("5000.00"))
                .baseCreditAmount(BigDecimal.ZERO)
                .costCenter("CC-001")
                .projectCode("PROJ-A")
                .department("Finance")
                .referenceType("INVOICE")
                .referenceId(50L)
                .taxCode("VAT-12")
                .taxAmount(new BigDecimal("600.00"))
                .build();

        // When
        JournalLineDto dto = journalLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getJournalEntryId());
        assertEquals(1, dto.getLineNumber());
        assertEquals(1L, dto.getAccountId());
        assertEquals("1110", dto.getAccountCode());
        assertEquals("Cash", dto.getAccountName());
        assertEquals("Cash debit line", dto.getDescription());
        assertEquals(new BigDecimal("5000.00"), dto.getDebitAmount());
        assertEquals(BigDecimal.ZERO, dto.getCreditAmount());
        assertEquals(new BigDecimal("5000.00"), dto.getBaseDebitAmount());
        assertEquals(BigDecimal.ZERO, dto.getBaseCreditAmount());
        assertEquals("CC-001", dto.getCostCenter());
        assertEquals("PROJ-A", dto.getProjectCode());
        assertEquals("Finance", dto.getDepartment());
        assertEquals("INVOICE", dto.getReferenceType());
        assertEquals(50L, dto.getReferenceId());
        assertEquals("VAT-12", dto.getTaxCode());
        assertEquals(new BigDecimal("600.00"), dto.getTaxAmount());
    }

    @Test
    void toDto_creditLine_mapsCorrectly() {
        // Given
        FiscalPeriod fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .name("January 2024")
                .periodNumber(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();

        JournalEntry entry = JournalEntry.builder()
                .id(100L)
                .entryNumber("JE-202401-0001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .description("Test entry")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .lines(new ArrayList<>())
                .build();

        Account account = Account.builder()
                .id(2L)
                .code("4100")
                .name("Sales Revenue")
                .accountType(AccountType.REVENUE)
                .normalBalance(NormalBalance.CREDIT)
                .childAccounts(new ArrayList<>())
                .build();

        JournalLine line = JournalLine.builder()
                .id(2L)
                .tenantId(1L)
                .journalEntry(entry)
                .lineNumber(2)
                .account(account)
                .description("Revenue credit line")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("5000.00"))
                .baseDebitAmount(BigDecimal.ZERO)
                .baseCreditAmount(new BigDecimal("5000.00"))
                .build();

        // When
        JournalLineDto dto = journalLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertEquals(100L, dto.getJournalEntryId());
        assertEquals(2, dto.getLineNumber());
        assertEquals(2L, dto.getAccountId());
        assertEquals("4100", dto.getAccountCode());
        assertEquals("Sales Revenue", dto.getAccountName());
        assertEquals("Revenue credit line", dto.getDescription());
        assertEquals(BigDecimal.ZERO, dto.getDebitAmount());
        assertEquals(new BigDecimal("5000.00"), dto.getCreditAmount());
    }

    @Test
    void toDto_withNullOptionalFields_handlesGracefully() {
        // Given
        FiscalPeriod fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .name("January 2024")
                .periodNumber(1)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();

        JournalEntry entry = JournalEntry.builder()
                .id(100L)
                .entryNumber("JE-202401-0001")
                .entryDate(LocalDate.of(2024, 1, 15))
                .fiscalPeriod(fiscalPeriod)
                .description("Test entry")
                .status(JournalStatus.DRAFT)
                .source(JournalSource.MANUAL)
                .lines(new ArrayList<>())
                .build();

        Account account = Account.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .childAccounts(new ArrayList<>())
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
                // Optional fields are null
                .costCenter(null)
                .projectCode(null)
                .department(null)
                .referenceType(null)
                .referenceId(null)
                .taxCode(null)
                .build();

        // When
        JournalLineDto dto = journalLineMapper.toDto(line);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCostCenter());
        assertNull(dto.getProjectCode());
        assertNull(dto.getDepartment());
        assertNull(dto.getReferenceType());
        assertNull(dto.getReferenceId());
        assertNull(dto.getTaxCode());
    }
}
