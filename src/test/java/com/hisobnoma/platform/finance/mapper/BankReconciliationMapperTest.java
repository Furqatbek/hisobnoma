package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.BankReconciliationDto;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.entity.BankAccount;
import com.hisobnoma.platform.finance.entity.BankReconciliation;
import com.hisobnoma.platform.finance.entity.ReconciliationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BankReconciliationMapperTest {

    @Autowired
    private BankReconciliationMapper bankReconciliationMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        BankAccount bankAccount = BankAccount.builder()
                .id(10L)
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .build();

        BankReconciliation entity = BankReconciliation.builder()
                .id(1L)
                .tenantId(1L)
                .bankAccount(bankAccount)
                .reconciliationNumber("REC-2024-001")
                .statementDate(LocalDate.of(2024, 1, 31))
                .statementStartDate(LocalDate.of(2024, 1, 1))
                .statementEndDate(LocalDate.of(2024, 1, 31))
                .status(ReconciliationStatus.DRAFT)
                .openingBalance(new BigDecimal("1000000.0000"))
                .statementEndingBalance(new BigDecimal("1500000.0000"))
                .bookBalance(new BigDecimal("1480000.0000"))
                .clearedDeposits(new BigDecimal("600000.0000"))
                .clearedWithdrawals(new BigDecimal("100000.0000"))
                .outstandingDeposits(new BigDecimal("20000.0000"))
                .outstandingWithdrawals(new BigDecimal("50000.0000"))
                .adjustedBankBalance(new BigDecimal("1470000.0000"))
                .adjustedBookBalance(new BigDecimal("1480000.0000"))
                .difference(new BigDecimal("10000.0000"))
                .transactionsCleared(25)
                .transactionsOutstanding(3)
                .notes("January reconciliation")
                .reconciledTransactions(new ArrayList<>())
                .build();

        // When
        BankReconciliationDto dto = bankReconciliationMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getBankAccountId());
        assertEquals("BANK-001", dto.getBankAccountCode());
        assertEquals("Main Bank Account", dto.getBankAccountName());
        assertEquals("REC-2024-001", dto.getReconciliationNumber());
        assertEquals(LocalDate.of(2024, 1, 31), dto.getStatementDate());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getStatementStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), dto.getStatementEndDate());
        assertEquals(ReconciliationStatus.DRAFT, dto.getStatus());
        assertEquals(new BigDecimal("1000000.0000"), dto.getOpeningBalance());
        assertEquals(new BigDecimal("1500000.0000"), dto.getStatementEndingBalance());
        assertEquals(new BigDecimal("1480000.0000"), dto.getBookBalance());
        assertEquals(new BigDecimal("600000.0000"), dto.getClearedDeposits());
        assertEquals(new BigDecimal("100000.0000"), dto.getClearedWithdrawals());
        assertEquals(new BigDecimal("20000.0000"), dto.getOutstandingDeposits());
        assertEquals(new BigDecimal("50000.0000"), dto.getOutstandingWithdrawals());
        assertEquals(new BigDecimal("1470000.0000"), dto.getAdjustedBankBalance());
        assertEquals(new BigDecimal("1480000.0000"), dto.getAdjustedBookBalance());
        assertEquals(new BigDecimal("10000.0000"), dto.getDifference());
        assertEquals(25, dto.getTransactionsCleared());
        assertEquals(3, dto.getTransactionsOutstanding());
        assertEquals("January reconciliation", dto.getNotes());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(10L)
                .statementDate(LocalDate.of(2024, 2, 29))
                .statementStartDate(LocalDate.of(2024, 2, 1))
                .statementEndDate(LocalDate.of(2024, 2, 29))
                .openingBalance(new BigDecimal("1500000.0000"))
                .statementEndingBalance(new BigDecimal("1800000.0000"))
                .notes("February reconciliation")
                .build();

        // When
        BankReconciliation entity = bankReconciliationMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(LocalDate.of(2024, 2, 29), entity.getStatementDate());
        assertEquals(LocalDate.of(2024, 2, 1), entity.getStatementStartDate());
        assertEquals(LocalDate.of(2024, 2, 29), entity.getStatementEndDate());
        assertEquals(new BigDecimal("1500000.0000"), entity.getOpeningBalance());
        assertEquals(new BigDecimal("1800000.0000"), entity.getStatementEndingBalance());
        assertEquals("February reconciliation", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getBankAccount());
        assertNull(entity.getReconciliationNumber());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        BankReconciliation existing = BankReconciliation.builder()
                .id(1L)
                .tenantId(1L)
                .reconciliationNumber("REC-001")
                .statementDate(LocalDate.of(2024, 1, 31))
                .statementStartDate(LocalDate.of(2024, 1, 1))
                .statementEndDate(LocalDate.of(2024, 1, 31))
                .openingBalance(new BigDecimal("1000000.0000"))
                .statementEndingBalance(new BigDecimal("1500000.0000"))
                .status(ReconciliationStatus.DRAFT)
                .reconciledTransactions(new ArrayList<>())
                .build();

        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(10L)
                .statementDate(LocalDate.of(2024, 1, 31))
                .statementStartDate(LocalDate.of(2024, 1, 1))
                .statementEndDate(LocalDate.of(2024, 1, 31))
                .openingBalance(new BigDecimal("1000000.0000"))
                .statementEndingBalance(new BigDecimal("1550000.0000"))
                .notes("Updated notes")
                .build();

        // When
        bankReconciliationMapper.updateEntity(request, existing);

        // Then
        assertEquals(new BigDecimal("1550000.0000"), existing.getStatementEndingBalance());
        assertEquals("Updated notes", existing.getNotes());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertEquals("REC-001", existing.getReconciliationNumber());
    }

    @Test
    void toDtoList_mapsAllReconciliations() {
        // Given
        BankAccount ba = BankAccount.builder()
                .id(10L).accountCode("BA-001").accountName("Bank").build();

        BankReconciliation r1 = BankReconciliation.builder()
                .id(1L).bankAccount(ba).reconciliationNumber("REC-001")
                .statementDate(LocalDate.of(2024, 1, 31))
                .statementStartDate(LocalDate.of(2024, 1, 1))
                .statementEndDate(LocalDate.of(2024, 1, 31))
                .openingBalance(BigDecimal.ZERO)
                .statementEndingBalance(BigDecimal.ZERO)
                .reconciledTransactions(new ArrayList<>()).build();

        BankReconciliation r2 = BankReconciliation.builder()
                .id(2L).bankAccount(ba).reconciliationNumber("REC-002")
                .statementDate(LocalDate.of(2024, 2, 29))
                .statementStartDate(LocalDate.of(2024, 2, 1))
                .statementEndDate(LocalDate.of(2024, 2, 29))
                .openingBalance(BigDecimal.ZERO)
                .statementEndingBalance(BigDecimal.ZERO)
                .reconciledTransactions(new ArrayList<>()).build();

        // When
        List<BankReconciliationDto> dtos = bankReconciliationMapper.toDtoList(List.of(r1, r2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
