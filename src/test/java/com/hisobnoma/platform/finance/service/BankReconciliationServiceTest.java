package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.BankReconciliationDto;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.dto.ReconcileTransactionsRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.BankReconciliationMapper;
import com.hisobnoma.platform.finance.mapper.BankTransactionMapper;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.finance.repository.BankReconciliationRepository;
import com.hisobnoma.platform.finance.repository.BankTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class BankReconciliationServiceTest {

    @Mock
    private BankReconciliationRepository reconciliationRepository;

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankReconciliationMapper reconciliationMapper;

    @Mock
    private BankTransactionMapper transactionMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private BankReconciliationService reconciliationService;

    private BankAccount bankAccount;
    private BankReconciliation reconciliation;
    private BankTransaction transaction1;
    private BankTransaction transaction2;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        bankAccount = BankAccount.builder()
                .id(1L)
                .accountCode("BANK-001")
                .accountName("Main Bank Account")
                .accountType(BankAccountType.CHECKING)
                .currentBalance(new BigDecimal("10000.00"))
                .tenantId(TENANT_ID)
                .active(true)
                .transactions(new ArrayList<>())
                .reconciliations(new ArrayList<>())
                .build();

        reconciliation = BankReconciliation.builder()
                .id(1L)
                .bankAccount(bankAccount)
                .reconciliationNumber("REC-000001")
                .statementDate(LocalDate.now())
                .statementStartDate(LocalDate.now().minusDays(30))
                .statementEndDate(LocalDate.now())
                .status(ReconciliationStatus.DRAFT)
                .openingBalance(new BigDecimal("8000.00"))
                .statementEndingBalance(new BigDecimal("10000.00"))
                .bookBalance(new BigDecimal("10000.00"))
                .clearedDeposits(BigDecimal.ZERO)
                .clearedWithdrawals(BigDecimal.ZERO)
                .outstandingDeposits(BigDecimal.ZERO)
                .outstandingWithdrawals(BigDecimal.ZERO)
                .difference(BigDecimal.ZERO)
                .transactionsCleared(0)
                .transactionsOutstanding(0)
                .tenantId(TENANT_ID)
                .reconciledTransactions(new ArrayList<>())
                .build();

        transaction1 = BankTransaction.builder()
                .id(1L)
                .bankAccount(bankAccount)
                .transactionNumber("BT-000001")
                .transactionDate(LocalDate.now().minusDays(5))
                .transactionType(BankTransactionType.DEPOSIT)
                .status(BankTransactionStatus.CLEARED)
                .description("Customer payment")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("1000.00"))
                .tenantId(TENANT_ID)
                .build();

        transaction2 = BankTransaction.builder()
                .id(2L)
                .bankAccount(bankAccount)
                .transactionNumber("BT-000002")
                .transactionDate(LocalDate.now().minusDays(3))
                .transactionType(BankTransactionType.WITHDRAWAL)
                .status(BankTransactionStatus.CLEARED)
                .description("Supplier payment")
                .debitAmount(new BigDecimal("500.00"))
                .creditAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();
    }

    @Test
    void shouldCreateReconciliationSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.hasInProgressReconciliation(1L, TENANT_ID)).thenReturn(false);
        when(bankAccountRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(bankAccount));
        when(reconciliationRepository.findMaxReconciliationNumberByTenantId(TENANT_ID)).thenReturn(null);
        when(reconciliationMapper.toEntity(any())).thenReturn(reconciliation);
        when(reconciliationRepository.save(any())).thenReturn(reconciliation);
        when(reconciliationMapper.toDto(reconciliation)).thenReturn(BankReconciliationDto.builder()
                .id(1L)
                .reconciliationNumber("REC-000001")
                .status(ReconciliationStatus.DRAFT)
                .build());

        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(1L)
                .statementDate(LocalDate.now())
                .statementStartDate(LocalDate.now().minusDays(30))
                .statementEndDate(LocalDate.now())
                .openingBalance(new BigDecimal("8000.00"))
                .statementEndingBalance(new BigDecimal("10000.00"))
                .build();

        // When
        BankReconciliationDto result = reconciliationService.createReconciliation(request);

        // Then
        assertNotNull(result);
        assertEquals("REC-000001", result.getReconciliationNumber());
        assertEquals(ReconciliationStatus.DRAFT, result.getStatus());
        verify(reconciliationRepository).save(any());
    }

    @Test
    void shouldFailWhenReconciliationAlreadyInProgress() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.hasInProgressReconciliation(1L, TENANT_ID)).thenReturn(true);

        CreateBankReconciliationRequest request = CreateBankReconciliationRequest.builder()
                .bankAccountId(1L)
                .statementDate(LocalDate.now())
                .statementStartDate(LocalDate.now().minusDays(30))
                .statementEndDate(LocalDate.now())
                .openingBalance(new BigDecimal("8000.00"))
                .statementEndingBalance(new BigDecimal("10000.00"))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> reconciliationService.createReconciliation(request));
        verify(reconciliationRepository, never()).save(any());
    }

    @Test
    void shouldStartReconciliationSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));
        when(reconciliationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reconciliationMapper.toDto(any())).thenReturn(BankReconciliationDto.builder()
                .id(1L)
                .status(ReconciliationStatus.IN_PROGRESS)
                .build());

        // When
        BankReconciliationDto result = reconciliationService.startReconciliation(1L);

        // Then
        assertNotNull(result);
        assertEquals(ReconciliationStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void shouldFailToStartNonDraftReconciliation() {
        // Given
        reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));

        // When/Then
        assertThrows(BusinessException.class, () -> reconciliationService.startReconciliation(1L));
    }

    @Test
    void shouldReconcileTransactionsSuccessfully() {
        // Given
        reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction1));
        when(transactionRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(transaction2));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByBankAccountIdAndStatusAndTenantId(any(), any(), any())).thenReturn(List.of());
        when(transactionRepository.findUnreconciledByBankAccountIdAndTenantId(any(), any(), any(), any())).thenReturn(List.of());
        when(reconciliationRepository.save(any())).thenReturn(reconciliation);
        when(reconciliationMapper.toDto(reconciliation)).thenReturn(BankReconciliationDto.builder()
                .id(1L)
                .status(ReconciliationStatus.IN_PROGRESS)
                .transactionsCleared(2)
                .build());

        ReconcileTransactionsRequest request = ReconcileTransactionsRequest.builder()
                .reconciliationId(1L)
                .transactionIds(List.of(1L, 2L))
                .clear(true)
                .build();

        // When
        BankReconciliationDto result = reconciliationService.reconcileTransactions(request);

        // Then
        assertNotNull(result);
        assertEquals(ReconciliationStatus.IN_PROGRESS, result.getStatus());
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void shouldFailWhenReconciliationCompleted() {
        // Given
        reconciliation.setStatus(ReconciliationStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));

        ReconcileTransactionsRequest request = ReconcileTransactionsRequest.builder()
                .reconciliationId(1L)
                .transactionIds(List.of(1L))
                .clear(true)
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> reconciliationService.reconcileTransactions(request));
    }

    @Test
    void shouldFailWhenTransactionBelongsToDifferentAccount() {
        // Given
        BankAccount differentAccount = BankAccount.builder()
                .id(2L)
                .accountCode("BANK-002")
                .build();
        transaction1.setBankAccount(differentAccount);

        reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));
        when(transactionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(transaction1));

        ReconcileTransactionsRequest request = ReconcileTransactionsRequest.builder()
                .reconciliationId(1L)
                .transactionIds(List.of(1L))
                .clear(true)
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> reconciliationService.reconcileTransactions(request));
    }

    @Test
    void shouldCancelReconciliationSuccessfully() {
        // Given
        reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        transaction1.setReconciliation(reconciliation);
        transaction1.setStatus(BankTransactionStatus.RECONCILED);
        reconciliation.getReconciledTransactions().add(transaction1);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reconciliationRepository.save(any())).thenReturn(reconciliation);
        when(reconciliationMapper.toDto(reconciliation)).thenReturn(BankReconciliationDto.builder()
                .id(1L)
                .status(ReconciliationStatus.CANCELLED)
                .build());

        // When
        BankReconciliationDto result = reconciliationService.cancelReconciliation(1L, "Test cancellation");

        // Then
        assertNotNull(result);
        assertEquals(ReconciliationStatus.CANCELLED, result.getStatus());
        verify(transactionRepository).save(transaction1);
    }

    @Test
    void shouldFailToCancelCompletedReconciliation() {
        // Given
        reconciliation.setStatus(ReconciliationStatus.COMPLETED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reconciliation));

        // When/Then
        assertThrows(BusinessException.class, () -> reconciliationService.cancelReconciliation(1L, "Test"));
    }

    @Test
    void shouldThrowNotFoundExceptionForNonExistentReconciliation() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(reconciliationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reconciliationService.getReconciliation(999L));
    }
}
