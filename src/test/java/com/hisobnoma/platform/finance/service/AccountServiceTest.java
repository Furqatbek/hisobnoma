package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.AccountDto;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import com.hisobnoma.platform.finance.mapper.AccountMapper;
import com.hisobnoma.platform.finance.repository.AccountRepository;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private AccountService accountService;

    private Account account;
    private AccountDto accountDto;
    private CreateAccountRequest createRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("1110")
                .name("Cash")
                .description("Cash account")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .accountLevel(1)
                .fullPath("1110")
                .systemAccount(false)
                .controlAccount(false)
                .allowsDirectPosting(true)
                .active(true)
                .currentBalance(new BigDecimal("10000.00"))
                .ytdDebit(BigDecimal.ZERO)
                .ytdCredit(BigDecimal.ZERO)
                .openingBalance(new BigDecimal("10000.00"))
                .currency("UZS")
                .childAccounts(new ArrayList<>())
                .build();

        accountDto = AccountDto.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .description("Cash account")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .accountLevel(1)
                .fullPath("1110")
                .systemAccount(false)
                .controlAccount(false)
                .allowsDirectPosting(true)
                .active(true)
                .currentBalance(new BigDecimal("10000.00"))
                .ytdDebit(BigDecimal.ZERO)
                .ytdCredit(BigDecimal.ZERO)
                .openingBalance(new BigDecimal("10000.00"))
                .currency("UZS")
                .build();

        createRequest = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .description("Cash account")
                .accountType(AccountType.ASSET)
                .allowsDirectPosting(true)
                .openingBalance(new BigDecimal("10000.00"))
                .build();
    }

    // ==================== getAccounts ====================

    @Test
    void getAccounts_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        // When
        PageResponse<AccountDto> result = accountService.getAccounts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("1110", result.getContent().get(0).getCode());
        verify(accountRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    // ==================== getAccount ====================

    @Test
    void getAccount_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        // When
        AccountDto result = accountService.getAccount(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("1110", result.getCode());
        assertEquals("Cash", result.getName());
        assertEquals(AccountType.ASSET, result.getAccountType());
    }

    @Test
    void getAccount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> accountService.getAccount(999L));
    }

    // ==================== createAccount ====================

    @Test
    void createAccount_success_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.existsByCodeAndTenantId("1110", TENANT_ID)).thenReturn(false);
        when(accountMapper.toEntity(createRequest)).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        // When
        AccountDto result = accountService.createAccount(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("1110", result.getCode());
        assertEquals("Cash", result.getName());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_duplicateCode_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.existsByCodeAndTenantId("1110", TENANT_ID)).thenReturn(true);

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.createAccount(createRequest));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(accountRepository, never()).save(any());
    }

    // ==================== updateAccount ====================

    @Test
    void updateAccount_success_updatesFields() {
        // Given
        CreateAccountRequest updateRequest = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash Updated")
                .accountType(AccountType.ASSET)
                .build();

        AccountDto updatedDto = AccountDto.builder()
                .id(1L)
                .code("1110")
                .name("Cash Updated")
                .accountType(AccountType.ASSET)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(any(Account.class))).thenReturn(updatedDto);

        // When
        AccountDto result = accountService.updateAccount(1L, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Cash Updated", result.getName());
        verify(accountMapper).updateEntity(eq(updateRequest), eq(account));
        verify(accountRepository).save(account);
    }

    @Test
    void updateAccount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> accountService.updateAccount(999L, createRequest));
    }

    @Test
    void updateAccount_systemAccount_throwsBusinessException() {
        // Given
        account.setSystemAccount(true);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.updateAccount(1L, createRequest));
        assertTrue(ex.getMessage().contains("System accounts cannot be modified"));
    }

    // ==================== deleteAccount ====================

    @Test
    void deleteAccount_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));

        // When
        accountService.deleteAccount(1L);

        // Then
        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_hasJournalLines_throwsBusinessException() {
        // Given
        account.setYtdDebit(new BigDecimal("5000.00"));
        account.setYtdCredit(new BigDecimal("3000.00"));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.deleteAccount(1L));
        assertTrue(ex.getMessage().contains("Cannot delete account with existing transactions"));
        verify(accountRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_systemAccount_throwsBusinessException() {
        // Given
        account.setSystemAccount(true);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.deleteAccount(1L));
        assertTrue(ex.getMessage().contains("System accounts cannot be deleted"));
    }

    @Test
    void deleteAccount_hasChildAccounts_throwsBusinessException() {
        // Given
        Account childAccount = Account.builder()
                .id(2L)
                .code("1111")
                .name("Petty Cash")
                .accountType(AccountType.ASSET)
                .build();
        account.getChildAccounts().add(childAccount);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(account));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.deleteAccount(1L));
        assertTrue(ex.getMessage().contains("Cannot delete account with child accounts"));
    }

    // ==================== getAccountsByType ====================

    @Test
    void getAccountsByType_returnsMatchingAccounts() {
        // Given
        Account liabilityAccount = Account.builder()
                .id(2L)
                .code("2110")
                .name("Accounts Payable")
                .accountType(AccountType.LIABILITY)
                .build();

        AccountDto liabilityDto = AccountDto.builder()
                .id(2L)
                .code("2110")
                .name("Accounts Payable")
                .accountType(AccountType.LIABILITY)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByAccountTypeAndTenantId(TENANT_ID, AccountType.ASSET))
                .thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        // When
        List<AccountDto> result = accountService.getAccountsByType(AccountType.ASSET);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AccountType.ASSET, result.get(0).getAccountType());
    }

    // ==================== getChildAccounts (getSubAccounts) ====================

    @Test
    void getChildAccounts_returnsSubAccounts() {
        // Given
        Account childAccount = Account.builder()
                .id(2L)
                .code("1111")
                .name("Petty Cash")
                .accountType(AccountType.ASSET)
                .build();
        AccountDto childDto = AccountDto.builder()
                .id(2L)
                .code("1111")
                .name("Petty Cash")
                .accountType(AccountType.ASSET)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findChildAccountsByParentId(TENANT_ID, 1L))
                .thenReturn(List.of(childAccount));
        when(accountMapper.toDtoList(List.of(childAccount))).thenReturn(List.of(childDto));

        // When
        List<AccountDto> result = accountService.getChildAccounts(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1111", result.get(0).getCode());
    }

    // ==================== getAccountTree (getAccountWithChildren) ====================

    @Test
    void getAccountWithChildren_returnsAccountTree() {
        // Given
        Account childAccount = Account.builder()
                .id(2L)
                .code("1111")
                .name("Petty Cash")
                .accountType(AccountType.ASSET)
                .childAccounts(new ArrayList<>())
                .build();
        account.getChildAccounts().add(childAccount);

        AccountDto treeDto = AccountDto.builder()
                .id(1L)
                .code("1110")
                .name("Cash")
                .childAccounts(List.of(AccountDto.builder()
                        .id(2L)
                        .code("1111")
                        .name("Petty Cash")
                        .build()))
                .build();

        when(accountRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(account));
        when(accountMapper.toDtoWithChildren(account)).thenReturn(treeDto);

        // When
        AccountDto result = accountService.getAccountWithChildren(1L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getChildAccounts());
        assertEquals(1, result.getChildAccounts().size());
        assertEquals("1111", result.getChildAccounts().get(0).getCode());
    }

    @Test
    void getAccountWithChildren_notFound_throwsNotFoundException() {
        // Given
        when(accountRepository.findByIdWithChildren(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> accountService.getAccountWithChildren(999L));
    }

    // ==================== updateAccountBalance ====================

    @Test
    void updateAccountBalance_updatesBalancesCorrectly() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        accountService.updateAccountBalance(1L, new BigDecimal("500.00"), BigDecimal.ZERO);

        // Then
        assertEquals(new BigDecimal("500.00"), account.getYtdDebit());
        assertEquals(BigDecimal.ZERO, account.getYtdCredit());
        verify(accountRepository).save(account);
    }

    // ==================== getAllAccounts ====================

    @Test
    void getAllAccounts_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        // When
        List<AccountDto> result = accountService.getAllAccounts();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== getRootAccounts ====================

    @Test
    void getRootAccounts_returnsOnlyRootAccounts() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findRootAccountsByTenantId(TENANT_ID)).thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(accountDto));

        // When
        List<AccountDto> result = accountService.getRootAccounts();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== getAccountByCode ====================

    @Test
    void getAccountByCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByCodeAndTenantId("1110", TENANT_ID)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        // When
        AccountDto result = accountService.getAccountByCode("1110");

        // Then
        assertNotNull(result);
        assertEquals("1110", result.getCode());
    }

    @Test
    void getAccountByCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findByCodeAndTenantId("9999", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> accountService.getAccountByCode("9999"));
    }

    // ==================== createAccount with parent ====================

    @Test
    void createAccount_withParent_setsParentRelationship() {
        // Given
        Account parentAccount = Account.builder()
                .id(10L)
                .tenantId(TENANT_ID)
                .code("1100")
                .name("Current Assets")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .accountLevel(1)
                .fullPath("1100")
                .childAccounts(new ArrayList<>())
                .build();

        CreateAccountRequest requestWithParent = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .parentAccountId(10L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.existsByCodeAndTenantId("1110", TENANT_ID)).thenReturn(false);
        when(accountMapper.toEntity(requestWithParent)).thenReturn(account);
        when(accountRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(parentAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(account)).thenReturn(accountDto);

        // When
        AccountDto result = accountService.createAccount(requestWithParent);

        // Then
        assertNotNull(result);
        verify(accountRepository).findByIdAndTenantId(10L, TENANT_ID);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_parentNotFound_throwsNotFoundException() {
        // Given
        CreateAccountRequest requestWithParent = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .parentAccountId(999L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.existsByCodeAndTenantId("1110", TENANT_ID)).thenReturn(false);
        when(accountMapper.toEntity(requestWithParent)).thenReturn(account);
        when(accountRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> accountService.createAccount(requestWithParent));
    }

    @Test
    void createAccount_parentTypeMismatch_throwsBusinessException() {
        // Given
        Account parentAccount = Account.builder()
                .id(10L)
                .tenantId(TENANT_ID)
                .code("2000")
                .name("Liabilities")
                .accountType(AccountType.LIABILITY)
                .normalBalance(NormalBalance.CREDIT)
                .accountLevel(1)
                .childAccounts(new ArrayList<>())
                .build();

        CreateAccountRequest requestWithParent = CreateAccountRequest.builder()
                .code("1110")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .parentAccountId(10L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.existsByCodeAndTenantId("1110", TENANT_ID)).thenReturn(false);
        when(accountMapper.toEntity(requestWithParent)).thenReturn(account);
        when(accountRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(parentAccount));

        // When/Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> accountService.createAccount(requestWithParent));
        assertTrue(ex.getMessage().contains("Parent account type must match"));
    }
}
