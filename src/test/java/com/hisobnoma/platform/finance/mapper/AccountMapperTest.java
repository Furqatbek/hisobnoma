package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.AccountDto;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AccountMapperTest {

    @Autowired
    private AccountMapper accountMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Account parentAccount = Account.builder()
                .id(10L)
                .code("1000")
                .name("Assets")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .childAccounts(new ArrayList<>())
                .build();

        Account account = Account.builder()
                .id(1L)
                .tenantId(1L)
                .code("1110")
                .name("Cash")
                .description("Cash account")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .parentAccount(parentAccount)
                .accountLevel(2)
                .fullPath("1000/1110")
                .systemAccount(false)
                .controlAccount(false)
                .allowsDirectPosting(true)
                .active(true)
                .currentBalance(new BigDecimal("10000.00"))
                .ytdDebit(new BigDecimal("5000.00"))
                .ytdCredit(new BigDecimal("2000.00"))
                .openingBalance(new BigDecimal("7000.00"))
                .currency("UZS")
                .taxCode("VAT-12")
                .sortOrder(1)
                .notes("Test notes")
                .childAccounts(new ArrayList<>())
                .build();

        // When
        AccountDto dto = accountMapper.toDto(account);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("1110", dto.getCode());
        assertEquals("Cash", dto.getName());
        assertEquals("Cash account", dto.getDescription());
        assertEquals(AccountType.ASSET, dto.getAccountType());
        assertEquals(NormalBalance.DEBIT, dto.getNormalBalance());
        assertEquals(10L, dto.getParentAccountId());
        assertEquals("1000", dto.getParentAccountCode());
        assertEquals("Assets", dto.getParentAccountName());
        assertEquals(2, dto.getAccountLevel());
        assertEquals("1000/1110", dto.getFullPath());
        assertFalse(dto.isSystemAccount());
        assertFalse(dto.isControlAccount());
        assertTrue(dto.isAllowsDirectPosting());
        assertTrue(dto.isActive());
        assertEquals(new BigDecimal("10000.00"), dto.getCurrentBalance());
        assertEquals(new BigDecimal("5000.00"), dto.getYtdDebit());
        assertEquals(new BigDecimal("2000.00"), dto.getYtdCredit());
        assertEquals(new BigDecimal("7000.00"), dto.getOpeningBalance());
        assertEquals("UZS", dto.getCurrency());
        assertEquals("VAT-12", dto.getTaxCode());
        assertEquals(1, dto.getSortOrder());
        assertEquals("Test notes", dto.getNotes());
    }

    @Test
    void toDto_withNullParent_mapsParentFieldsToNull() {
        // Given
        Account account = Account.builder()
                .id(1L)
                .code("1000")
                .name("Assets")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .parentAccount(null)
                .accountLevel(1)
                .active(true)
                .childAccounts(new ArrayList<>())
                .build();

        // When
        AccountDto dto = accountMapper.toDto(account);

        // Then
        assertNotNull(dto);
        assertNull(dto.getParentAccountId());
        assertNull(dto.getParentAccountCode());
        assertNull(dto.getParentAccountName());
    }

    @Test
    void fromCreateRequest_toEntity_mapsBasicFields() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1120")
                .name("Bank Accounts")
                .description("Bank account description")
                .accountType(AccountType.ASSET)
                .controlAccount(true)
                .allowsDirectPosting(false)
                .openingBalance(new BigDecimal("50000.00"))
                .currency("USD")
                .taxCode("TAX-01")
                .bankAccountNumber("1234567890")
                .bankName("National Bank")
                .sortOrder(2)
                .notes("Bank notes")
                .build();

        // When
        Account entity = accountMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("1120", entity.getCode());
        assertEquals("Bank Accounts", entity.getName());
        assertEquals("Bank account description", entity.getDescription());
        assertEquals(AccountType.ASSET, entity.getAccountType());
        assertTrue(entity.isControlAccount());
        assertFalse(entity.isAllowsDirectPosting());
        assertEquals(new BigDecimal("50000.00"), entity.getOpeningBalance());
        assertEquals("USD", entity.getCurrency());
        assertEquals("TAX-01", entity.getTaxCode());
        assertEquals("1234567890", entity.getBankAccountNumber());
        assertEquals("National Bank", entity.getBankName());
        assertEquals(2, entity.getSortOrder());
        assertEquals("Bank notes", entity.getNotes());
        // Ignored fields should not be set
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
    }

    @Test
    void updateEntity_updatesFieldsOnExistingAccount() {
        // Given
        Account existing = Account.builder()
                .id(1L)
                .tenantId(1L)
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .active(true)
                .currentBalance(BigDecimal.ZERO)
                .ytdDebit(BigDecimal.ZERO)
                .ytdCredit(BigDecimal.ZERO)
                .childAccounts(new ArrayList<>())
                .build();

        CreateAccountRequest request = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash Updated")
                .description("Updated description")
                .accountType(AccountType.ASSET)
                .build();

        // When
        accountMapper.updateEntity(request, existing);

        // Then
        assertEquals("Cash Updated", existing.getName());
        assertEquals("Updated description", existing.getDescription());
        // ID and tenantId should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }
}
