package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CashFlowDto;
import com.hisobnoma.platform.finance.dto.CreateBankTransactionRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.BankTransactionMapper;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.finance.repository.BankTransactionRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankTransactionServiceTest {

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankTransactionMapper bankTransactionMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private BankTransactionService bankTransactionService;

    private BankAccount bankAccount;
    private BankTransaction transaction;
    private BankTransactionDto transactionDto;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        bankAccount = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .accountType(BankAccountType.CHECKING)
                .currentBalance(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("10000.00"))
                .currency("UZS")
                .active(true)
                .tenantId(TENANT_ID)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();

        transaction = BankTransaction.builder()
                .id(1L)
                .bankAccount(bankAccount)
                .transactionNumber("BT-000001")
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .description("Customer payment")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("500.00"))
                .runningBalance(new BigDecimal("10500.00"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(new BigDecimal("500.00"))
                .tenantId(TENANT_ID)
                .build();

        transactionDto = BankTransactionDto.builder()
                .id(1L)
                .bankAccountId(1L)
                .bankAccountCode("BANK-001")
                .transactionNumber("BT-000001")
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.PENDING)
                .description("Customer payment")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("500.00"))
                .runningBalance(new BigDecimal("10500.00"))
                .build();
    }

    // ====== getTransactions ======

    @Test
    void getTransactions_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<BankTransaction> page = new PageImpl<>(List.of(transaction), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(bankTransactionMapper.toDto(transaction)).thenReturn(transactionDto);

        // When
        var result = bankTransactionService.getTransactions(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("BT-000001", result.getContent().get(0).getTransactionNumber());
    }

    // ====== getTransaction ======

    @Test
    void getTransaction_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(bankTransactionMapper.toDto(transaction)).thenReturn(transactionDto);

        // When
        BankTransactionDto result = bankTransactionService.getTransaction(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("BT-000001", result.getTransactionNumber());
    }

    @Test
    void getTransaction_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> bankTransactionService.getTransaction(999L));
    }

    // ====== createTransaction ======

    @Test
    void createBankTransaction_deposit_success() {
        // Given
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(1L)
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Customer payment")
                .amount(new BigDecimal("500.00"))
                .isDebit(false)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));
        when(bankTransactionMapper.toEntity(request)).thenReturn(transaction);
        when(bankTransactionRepository.findMaxTransactionNumberByTenantId(TENANT_ID)).thenReturn(null);
        when(bankTransactionRepository.save(any(BankTransaction.class))).thenReturn(transaction);
        when(bankTransactionMapper.toDto(transaction)).thenReturn(transactionDto);

        // When
        BankTransactionDto result = bankTransactionService.createTransaction(request);

        // Then
        assertNotNull(result);
        assertEquals(BankTransactionType.DEPOSIT, result.getTransactionType());
        verify(bankTransactionRepository).save(any(BankTransaction.class));
        verify(bankAccountRepository).save(bankAccount);
    }

    @Test
    void createBankTransaction_withdrawal_success() {
        // Given
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(1L)
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.WITHDRAWAL)
                .description("Supplier payment")
                .amount(new BigDecimal("200.00"))
                .isDebit(true)
                .build();

        BankTransaction withdrawalTx = BankTransaction.builder()
                .id(2L)
                .bankAccount(bankAccount)
                .transactionNumber("BT-000002")
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.WITHDRAWAL)
                .status(BankTransactionStatus.PENDING)
                .description("Supplier payment")
                .debitAmount(new BigDecimal("200.00"))
                .creditAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        BankTransactionDto withdrawalDto = BankTransactionDto.builder()
                .id(2L)
                .transactionType(BankTransactionType.WITHDRAWAL)
                .debitAmount(new BigDecimal("200.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));
        when(bankTransactionMapper.toEntity(request)).thenReturn(withdrawalTx);
        when(bankTransactionRepository.findMaxTransactionNumberByTenantId(TENANT_ID)).thenReturn("BT-000001");
        when(bankTransactionRepository.save(any(BankTransaction.class))).thenReturn(withdrawalTx);
        when(bankTransactionMapper.toDto(withdrawalTx)).thenReturn(withdrawalDto);

        // When
        BankTransactionDto result = bankTransactionService.createTransaction(request);

        // Then
        assertNotNull(result);
        assertEquals(BankTransactionType.WITHDRAWAL, result.getTransactionType());
        verify(bankTransactionRepository).save(any(BankTransaction.class));
    }

    @Test
    void createBankTransaction_accountNotFound_throwsNotFoundException() {
        // Given
        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(999L)
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Payment")
                .amount(new BigDecimal("500.00"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> bankTransactionService.createTransaction(request));
        verify(bankTransactionRepository, never()).save(any());
    }

    @Test
    void createBankTransaction_inactiveAccount_throwsBusinessException() {
        // Given
        bankAccount.setActive(false);

        CreateBankTransactionRequest request = CreateBankTransactionRequest.builder()
                .bankAccountId(1L)
                .transactionDate(LocalDate.now())
                .transactionType(BankTransactionType.DEPOSIT)
                .description("Payment")
                .amount(new BigDecimal("500.00"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));

        // When/Then
        assertThrows(BusinessException.class, () -> bankTransactionService.createTransaction(request));
        verify(bankTransactionRepository, never()).save(any());
    }

    // ====== getTransactionsByAccount ======

    @Test
    void getTransactionsByAccount_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<BankTransaction> page = new PageImpl<>(List.of(transaction), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByBankAccountIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(bankTransactionMapper.toDto(transaction)).thenReturn(transactionDto);

        // When
        var result = bankTransactionService.getTransactionsByBankAccount(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== getCashFlow ======

    @Test
    void getCashFlow_returnsCorrectCalculation() {
        // Given
        BankTransaction depositTx = BankTransaction.builder()
                .id(1L)
                .bankAccount(bankAccount)
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.CLEARED)
                .creditAmount(new BigDecimal("1000.00"))
                .debitAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        BankTransaction withdrawalTx = BankTransaction.builder()
                .id(2L)
                .bankAccount(bankAccount)
                .transactionType(BankTransactionType.WITHDRAWAL)
                .status(BankTransactionStatus.CLEARED)
                .debitAmount(new BigDecimal("300.00"))
                .creditAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));
        when(bankTransactionRepository.findByBankAccountIdAndDateRangeAndTenantId(1L, startDate, endDate, TENANT_ID))
                .thenReturn(List.of(depositTx, withdrawalTx));

        // When
        CashFlowDto result = bankTransactionService.getCashFlow(1L, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getTotalInflows());
        assertEquals(new BigDecimal("300.00"), result.getTotalOutflows());
        assertEquals(new BigDecimal("700.00"), result.getNetCashFlow());
        assertEquals(new BigDecimal("10000.00"), result.getClosingBalance());
    }

    // ====== voidTransaction ======

    @Test
    void voidTransaction_pendingTransaction_success() {
        // Given
        transaction.setStatus(BankTransactionStatus.PENDING);
        BankTransactionDto voidedDto = BankTransactionDto.builder()
                .id(1L)
                .status(BankTransactionStatus.VOIDED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(bankTransactionRepository.save(any(BankTransaction.class))).thenReturn(transaction);
        when(bankTransactionMapper.toDto(any(BankTransaction.class))).thenReturn(voidedDto);

        // When
        BankTransactionDto result = bankTransactionService.voidTransaction(1L, "Test void");

        // Then
        assertNotNull(result);
        assertEquals(BankTransactionStatus.VOIDED, result.getStatus());
        verify(bankAccountRepository).save(bankAccount);
    }

    @Test
    void voidTransaction_reconciledTransaction_throwsBusinessException() {
        // Given
        transaction.setStatus(BankTransactionStatus.RECONCILED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> bankTransactionService.voidTransaction(1L, "Cannot void"));
        verify(bankTransactionRepository, never()).save(any());
    }

    @Test
    void voidTransaction_alreadyVoided_throwsBusinessException() {
        // Given
        transaction.setStatus(BankTransactionStatus.VOIDED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> bankTransactionService.voidTransaction(1L, "Already voided"));
    }

    // ====== clearTransaction ======

    @Test
    void clearTransaction_pendingTransaction_becomesCleared() {
        // Given
        transaction.setStatus(BankTransactionStatus.PENDING);
        BankTransactionDto clearedDto = BankTransactionDto.builder()
                .id(1L)
                .status(BankTransactionStatus.CLEARED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));
        when(bankTransactionRepository.save(any(BankTransaction.class))).thenReturn(transaction);
        when(bankTransactionMapper.toDto(any(BankTransaction.class))).thenReturn(clearedDto);

        // When
        BankTransactionDto result = bankTransactionService.clearTransaction(1L);

        // Then
        assertNotNull(result);
        assertEquals(BankTransactionStatus.CLEARED, result.getStatus());
    }

    @Test
    void clearTransaction_alreadyCleared_throwsBusinessException() {
        // Given
        transaction.setStatus(BankTransactionStatus.CLEARED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(bankTransactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction));

        // When/Then
        assertThrows(BusinessException.class, () -> bankTransactionService.clearTransaction(1L));
    }
}
