package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.BankAccountDto;
import com.hisobnoma.platform.finance.dto.CreateBankAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.BankAccount;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountMapperTest {

    private BankAccountMapper bankAccountMapper;

    @BeforeEach
    void setUp() {
        bankAccountMapper = Mappers.getMapper(BankAccountMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        Account glAccount = Account.builder()
                .id(10L)
                .code("1001")
                .name("Cash at Bank")
                .build();

        BankAccount entity = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .description("Primary operating account")
                .accountType(BankAccountType.CHECKING)
                .bankName("National Bank")
                .currency("UZS")
                .currentBalance(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("10000.00"))
                .openingBalance(new BigDecimal("5000.00"))
                .active(true)
                .defaultAccount(false)
                .glAccount(glAccount)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();
        entity.setTenantId(1L);

        // When
        BankAccountDto dto = bankAccountMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("BANK-001", dto.getAccountCode());
        assertEquals("Main Bank Account", dto.getAccountName());
        assertEquals(BankAccountType.CHECKING, dto.getAccountType());
        assertEquals("National Bank", dto.getBankName());
        assertEquals("UZS", dto.getCurrency());
        assertEquals(new BigDecimal("10000.00"), dto.getCurrentBalance());
        assertTrue(dto.isActive());
        assertEquals(10L, dto.getGlAccountId());
        assertEquals("1001", dto.getGlAccountCode());
        assertEquals("Cash at Bank", dto.getGlAccountName());
    }

    @Test
    void toDto_nullGlAccount_handledGracefully() {
        // Given
        BankAccount entity = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Test Account")
                .accountType(BankAccountType.CHECKING)
                .glAccount(null)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();

        // When
        BankAccountDto dto = bankAccountMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNull(dto.getGlAccountId());
        assertNull(dto.getGlAccountCode());
    }

    @Test
    void toEntity_mapsRequestToEntity() {
        // Given
        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BANK-002")
                .accountName("Savings Account")
                .accountType(BankAccountType.SAVINGS)
                .bankName("Savings Bank")
                .currency("USD")
                .build();

        // When
        BankAccount entity = bankAccountMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("BANK-002", entity.getAccountCode());
        assertEquals("Savings Account", entity.getAccountName());
        assertEquals(BankAccountType.SAVINGS, entity.getAccountType());
        assertEquals("USD", entity.getCurrency());
        assertNull(entity.getId());
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        BankAccount entity1 = BankAccount.builder()
                .id(1L).accountCode("BANK-001").accountName("Acc 1")
                .accountType(BankAccountType.CHECKING)
                .transactions(new ArrayList<>()).reconciliations(new ArrayList<>())
                .build();
        BankAccount entity2 = BankAccount.builder()
                .id(2L).accountCode("BANK-002").accountName("Acc 2")
                .accountType(BankAccountType.SAVINGS)
                .transactions(new ArrayList<>()).reconciliations(new ArrayList<>())
                .build();

        // When
        List<BankAccountDto> dtos = bankAccountMapper.toDtoList(List.of(entity1, entity2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }

    @Test
    void updateEntity_updatesExistingEntity() {
        // Given
        BankAccount entity = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Old Name")
                .accountType(BankAccountType.CHECKING)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();

        CreateBankAccountRequest request = CreateBankAccountRequest.builder()
                .accountCode("BANK-001")
                .accountName("New Name")
                .accountType(BankAccountType.SAVINGS)
                .bankName("Updated Bank")
                .build();

        // When
        bankAccountMapper.updateEntity(request, entity);

        // Then
        assertEquals("New Name", entity.getAccountName());
        assertEquals(BankAccountType.SAVINGS, entity.getAccountType());
        assertEquals(1L, entity.getId()); // id should not change
    }
}
