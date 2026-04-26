package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CreateBankTransactionRequest;
import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankTransactionMapperTest {

    private BankTransactionMapper bankTransactionMapper;

    @BeforeEach
    void setUp() {
        bankTransactionMapper = Mappers.getMapper(BankTransactionMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        BankAccount bankAccount = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Account")
                .accountType(BankAccountType.CHECKING)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();

        Account counterAccount = Account.builder()
                .id(10L)
                .code("4000")
                .name("Sales Revenue")
                .build();

        BankTransaction entity = BankTransaction.builder()
                .id(1L)
                .bankAccount(bankAccount)
                .transactionNumber("BT-000001")
                .transactionDate(LocalDate.of(2024, 6, 15))
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.CLEARED)
                .description("Customer payment")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("1000.00"))
                .runningBalance(new BigDecimal("11000.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("1000.00"))
                .counterAccount(counterAccount)
                .build();
        entity.setTenantId(1L);

        // When
        BankTransactionDto dto = bankTransactionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(1L, dto.getBankAccountId());
        assertEquals("BANK-001", dto.getBankAccountCode());
        assertEquals("Main Account", dto.getBankAccountName());
        assertEquals("BT-000001", dto.getTransactionNumber());
        assertEquals(BankTransactionType.DEPOSIT, dto.getTransactionType());
        assertEquals(BankTransactionStatus.CLEARED, dto.getStatus());
        assertEquals(new BigDecimal("1000.00"), dto.getCreditAmount());
        assertEquals(10L, dto.getCounterAccountId());
        assertEquals("4000", dto.getCounterAccountCode());
    }

    @Test
    void toDto_nullRelations_handledGracefully() {
        // Given
        BankAccount bankAccount = BankAccount.builder()
                .id(1L).accountCode("BANK-001").accountName("Main")
                .accountType(BankAccountType.CHECKING)
                .transactions(new ArrayList<>()).reconciliations(new ArrayList<>())
                .build();

        BankTransaction entity = BankTransaction.builder()
                .id(1L)
                .bankAccount(bankAccount)
                .transactionNumber("BT-000001")
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .description("Test")
                .counterAccount(null)
                .transferToAccount(null)
                .reconciliation(null)
                .build();

        // When
        BankTransactionDto dto = bankTransactionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCounterAccountId());
        assertNull(dto.getTransferToAccountId());
        assertNull(dto.getReconciliationId());
    }

    @Test
    void toEntity_mapsRequestToEntity() {
        // Given
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(1L)
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Customer payment")
                .amount(new BigDecimal("500.00"))
                .currency("UZS")
                .build();

        // When
        BankTransaction entity = bankTransactionMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(BankTransactionType.DEPOSIT, entity.getTransactionType());
        assertEquals("Customer payment", entity.getDescription());
        assertNull(entity.getId());
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        BankAccount bankAccount = BankAccount.builder()
                .id(1L).accountCode("BANK-001").accountName("Main")
                .accountType(BankAccountType.CHECKING)
                .transactions(new ArrayList<>()).reconciliations(new ArrayList<>())
                .build();

        BankTransaction tx1 = BankTransaction.builder()
                .id(1L).bankAccount(bankAccount).transactionNumber("BT-000001")
                .transactionDate(LocalDate.now()).transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.CLEARED).description("Tx1").build();
        BankTransaction tx2 = BankTransaction.builder()
                .id(2L).bankAccount(bankAccount).transactionNumber("BT-000002")
                .transactionDate(LocalDate.now()).transactionType(BankTransactionType.WITHDRAWAL)
                .status(BankTransactionStatus.PENDING).description("Tx2").build();

        // When
        List<BankTransactionDto> dtos = bankTransactionMapper.toDtoList(List.of(tx1, tx2));

        // Then
        assertEquals(2, dtos.size());
    }
}
