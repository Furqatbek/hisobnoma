package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.BankAccountDto;
import com.hisobnoma.platform.finance.dto.CreateBankAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.BankAccount;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import com.hisobnoma.platform.finance.mapper.BankAccountMapper;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private BankAccountService bankAccountService;

    private BankAccount bankAccount;
    private BankAccountDto bankAccountDto;
    private CreateBankAccountRequest createRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        bankAccount = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .accountType(BankAccountType.CHECKING)
                .bankName("National Bank")
                .currentBalance(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("10000.00"))
                .openingBalance(new BigDecimal("5000.00"))
                .currency("UZS")
                .active(true)
                .defaultAccount(false)
                .tenantId(TENANT_ID)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();

        bankAccountDto = BankAccountDto.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .accountType(BankAccountType.CHECKING)
                .bankName("National Bank")
                .currentBalance(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("10000.00"))
                .openingBalance(new BigDecimal("5000.00"))
                .currency("UZS")
                .active(true)
                .defaultAccount(false)
                .build();

        createRequest = CreateBankAccountRequest.builder()
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .accountType(BankAccountType.CHECKING)
                .bankName("National Bank")
                .openingBalance(new BigDecimal("5000.00"))
                .currency("UZS")
                .build();
    }

    // ====== getBankAccounts ======

    @Test
    void getBankAccounts_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<BankAccount> page = new PageImpl<>(List.of(bankAccount), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(bankAccountMapper.toDto(bankAccount)).thenReturn(bankAccountDto);

        // When
        var result = bankAccountService.getBankAccounts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("BANK-001", result.getContent().get(0).getAccountCode());
        verify(bankAccountRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    // ====== getBankAccount ======

    @Test
    void getBankAccount_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));
        when(bankAccountMapper.toDto(bankAccount)).thenReturn(bankAccountDto);

        // When
        BankAccountDto result = bankAccountService.getBankAccount(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("BANK-001", result.getAccountCode());
    }

    @Test
    void getBankAccount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> bankAccountService.getBankAccount(999L));
    }

    // ====== createBankAccount ======

    @Test
    void createBankAccount_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.existsByAccountCodeAndTenantId("BANK-001", TENANT_ID)).thenReturn(false);
        when(bankAccountMapper.toEntity(createRequest)).thenReturn(bankAccount);
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(bankAccount);
        when(bankAccountMapper.toDto(bankAccount)).thenReturn(bankAccountDto);

        // When
        BankAccountDto result = bankAccountService.createBankAccount(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("BANK-001", result.getAccountCode());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void createBankAccount_duplicateAccountNumber_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.existsByAccountCodeAndTenantId("BANK-001", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> bankAccountService.createBankAccount(createRequest));
        verify(bankAccountRepository, never()).save(any());
    }

    // ====== updateBankAccount ======

    @Test
    void updateBankAccount_success() {
        // Given
        CreateBankAccountRequest updateRequest = CreateBankAccountRequest.builder()
                .accountCode("BANK-001")
                .accountName("Updated Bank Account")
                .accountType(BankAccountType.CHECKING)
                .bankName("National Bank Updated")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(bankAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(bankAccountDto);

        // When
        BankAccountDto result = bankAccountService.updateBankAccount(1L, updateRequest);

        // Then
        assertNotNull(result);
        verify(bankAccountMapper).updateEntity(eq(updateRequest), any(BankAccount.class));
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    // ====== getActiveBankAccounts ======

    @Test
    void getActiveAccounts_returnsOnlyActive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(bankAccount));
        when(bankAccountMapper.toDtoList(List.of(bankAccount))).thenReturn(List.of(bankAccountDto));

        // When
        List<BankAccountDto> result = bankAccountService.getActiveBankAccounts();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== getAccountsByType ======

    @Test
    void getAccountsByType_returnsFilteredAccounts() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByAccountTypeAndTenantId(BankAccountType.CHECKING, TENANT_ID))
                .thenReturn(List.of(bankAccount));
        when(bankAccountMapper.toDtoList(List.of(bankAccount))).thenReturn(List.of(bankAccountDto));

        // When
        List<BankAccountDto> result = bankAccountService.getBankAccountsByType(BankAccountType.CHECKING);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BankAccountType.CHECKING, result.get(0).getAccountType());
    }

    // ====== deactivateBankAccount ======

    @Test
    void deactivateBankAccount_success() {
        // Given
        BankAccount zeroBalanceAccount = BankAccount.builder()
                .id(2L)
                .accountCode("BANK-002")
                .accountName("Secondary Account")
                .accountType(BankAccountType.SAVINGS)
                .currentBalance(BigDecimal.ZERO)
                .active(true)
                .defaultAccount(false)
                .tenantId(TENANT_ID)
                .build();

        BankAccountDto deactivatedDto = BankAccountDto.builder()
                .id(2L)
                .accountCode("BANK-002")
                .active(false)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(zeroBalanceAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(zeroBalanceAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(deactivatedDto);

        // When
        BankAccountDto result = bankAccountService.deactivateBankAccount(2L);

        // Then
        assertNotNull(result);
        assertFalse(result.isActive());
    }

    @Test
    void deactivateBankAccount_alreadyInactive_idempotent() {
        // Given - account with zero balance, not default, but already inactive
        BankAccount inactiveAccount = BankAccount.builder()
                .id(2L)
                .accountCode("BANK-002")
                .accountName("Inactive Account")
                .accountType(BankAccountType.SAVINGS)
                .currentBalance(BigDecimal.ZERO)
                .active(false)
                .defaultAccount(false)
                .tenantId(TENANT_ID)
                .build();

        BankAccountDto inactiveDto = BankAccountDto.builder()
                .id(2L)
                .accountCode("BANK-002")
                .active(false)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(inactiveAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(inactiveAccount);
        when(bankAccountMapper.toDto(any(BankAccount.class))).thenReturn(inactiveDto);

        // When
        BankAccountDto result = bankAccountService.deactivateBankAccount(2L);

        // Then
        assertNotNull(result);
        assertFalse(result.isActive());
    }

    @Test
    void deactivateBankAccount_defaultAccount_throwsBusinessException() {
        // Given
        bankAccount.setDefaultAccount(true);
        bankAccount.setCurrentBalance(BigDecimal.ZERO);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));

        // When/Then
        assertThrows(BusinessException.class, () -> bankAccountService.deactivateBankAccount(1L));
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void deactivateBankAccount_nonZeroBalance_throwsBusinessException() {
        // Given - account with non-zero balance
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));

        // When/Then
        assertThrows(BusinessException.class, () -> bankAccountService.deactivateBankAccount(1L));
        verify(bankAccountRepository, never()).save(any());
    }

    // ====== createBankAccount with GL account ======

    @Test
    void createBankAccount_withGlAccount_success() {
        // Given
        Account glAccount = Account.builder()
                .id(10L)
                .code("1001")
                .name("Cash at Bank")
                .tenantId(TENANT_ID)
                .build();

        createRequest.setGlAccountId(10L);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.existsByAccountCodeAndTenantId("BANK-001", TENANT_ID)).thenReturn(false);
        when(bankAccountMapper.toEntity(createRequest)).thenReturn(bankAccount);
        when(accountRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(glAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(bankAccount);
        when(bankAccountMapper.toDto(bankAccount)).thenReturn(bankAccountDto);

        // When
        BankAccountDto result = bankAccountService.createBankAccount(createRequest);

        // Then
        assertNotNull(result);
        verify(accountRepository).findByIdAndTenantId(10L, TENANT_ID);
    }

    // ====== updateBalance ======

    @Test
    void updateBalance_updatesAccountBalance() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));

        // When
        bankAccountService.updateBalance(1L, new BigDecimal("500.00"));

        // Then
        verify(bankAccountRepository).save(bankAccount);
        assertEquals(new BigDecimal("10500.00"), bankAccount.getCurrentBalance());
    }
}
