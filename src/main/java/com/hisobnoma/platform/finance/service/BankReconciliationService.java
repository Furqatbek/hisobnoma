package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.BankReconciliationDto;
import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.dto.ReconcileTransactionsRequest;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.BankReconciliationMapper;
import com.hisobnoma.platform.finance.mapper.BankTransactionMapper;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.finance.repository.BankReconciliationRepository;
import com.hisobnoma.platform.finance.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankReconciliationService {

    private final BankReconciliationRepository reconciliationRepository;
    private final BankTransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankReconciliationMapper reconciliationMapper;
    private final BankTransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<BankReconciliationDto> getReconciliations(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<BankReconciliation> page = reconciliationRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(reconciliationMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<BankReconciliationDto> getReconciliationsByBankAccount(Long bankAccountId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<BankReconciliation> page = reconciliationRepository.findByBankAccountIdAndTenantId(bankAccountId, tenantId, pageable);
        return PageResponse.of(page.map(reconciliationMapper::toDto));
    }

    @Transactional(readOnly = true)
    public BankReconciliationDto getReconciliation(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankReconciliation reconciliation = reconciliationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank reconciliation not found with id: " + id));
        return reconciliationMapper.toDto(reconciliation);
    }

    @Transactional(readOnly = true)
    public List<BankTransactionDto> getUnreconciledTransactions(Long bankAccountId, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankTransactionStatus> statuses = Arrays.asList(
                BankTransactionStatus.PENDING, BankTransactionStatus.CLEARED);
        List<BankTransaction> transactions = transactionRepository.findUnreconciledByBankAccountIdAndTenantId(
                bankAccountId, statuses, endDate, tenantId);
        return transactionMapper.toDtoList(transactions);
    }

    @Transactional
    public BankReconciliationDto createReconciliation(CreateBankReconciliationRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check if there's already an in-progress reconciliation
        if (reconciliationRepository.hasInProgressReconciliation(request.getBankAccountId(), tenantId)) {
            throw new BusinessException("There is already an in-progress reconciliation for this account");
        }

        BankAccount bankAccount = bankAccountRepository.findByIdAndTenantId(request.getBankAccountId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        BankReconciliation reconciliation = reconciliationMapper.toEntity(request);
        reconciliation.setTenantId(tenantId);
        reconciliation.setBankAccount(bankAccount);
        reconciliation.setReconciliationNumber(generateReconciliationNumber(tenantId));
        reconciliation.setStatus(ReconciliationStatus.DRAFT);
        reconciliation.setBookBalance(bankAccount.getCurrentBalance());

        // Initialize all amounts to zero
        reconciliation.setClearedDeposits(BigDecimal.ZERO);
        reconciliation.setClearedWithdrawals(BigDecimal.ZERO);
        reconciliation.setOutstandingDeposits(BigDecimal.ZERO);
        reconciliation.setOutstandingWithdrawals(BigDecimal.ZERO);
        reconciliation.setTransactionsCleared(0);
        reconciliation.setTransactionsOutstanding(0);

        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    @Transactional
    public BankReconciliationDto startReconciliation(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankReconciliation reconciliation = reconciliationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank reconciliation not found"));

        if (reconciliation.getStatus() != ReconciliationStatus.DRAFT) {
            throw new BusinessException("Only draft reconciliations can be started");
        }

        reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    @Transactional
    public BankReconciliationDto reconcileTransactions(ReconcileTransactionsRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankReconciliation reconciliation = reconciliationRepository.findByIdAndTenantId(request.getReconciliationId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Bank reconciliation not found"));

        if (reconciliation.getStatus() == ReconciliationStatus.COMPLETED) {
            throw new BusinessException("Cannot modify a completed reconciliation");
        }

        if (reconciliation.getStatus() == ReconciliationStatus.DRAFT) {
            reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        }

        // Process each transaction
        for (Long transactionId : request.getTransactionIds()) {
            BankTransaction transaction = transactionRepository.findByIdAndTenantId(transactionId, tenantId)
                    .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

            if (!transaction.getBankAccount().getId().equals(reconciliation.getBankAccount().getId())) {
                throw new BusinessException("Transaction does not belong to this bank account");
            }

            if (request.isClear()) {
                // Mark as reconciled
                transaction.setStatus(BankTransactionStatus.RECONCILED);
                transaction.setReconciliation(reconciliation);
                transaction.setReconciledDate(LocalDate.now());
            } else {
                // Unmark
                if (transaction.getReconciliation() != null &&
                    transaction.getReconciliation().getId().equals(reconciliation.getId())) {
                    transaction.setStatus(BankTransactionStatus.CLEARED);
                    transaction.setReconciliation(null);
                    transaction.setReconciledDate(null);
                }
            }
            transactionRepository.save(transaction);
        }

        // Recalculate reconciliation totals
        recalculateTotals(reconciliation);
        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    @Transactional
    public BankReconciliationDto completeReconciliation(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        BankReconciliation reconciliation = reconciliationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank reconciliation not found"));

        if (reconciliation.getStatus() == ReconciliationStatus.COMPLETED) {
            throw new BusinessException("Reconciliation is already completed");
        }

        // Recalculate and verify balance
        recalculateTotals(reconciliation);

        if (!reconciliation.isBalanced()) {
            throw new BusinessException("Reconciliation is not balanced. Difference: " + reconciliation.getDifference());
        }

        // Update bank account with reconciliation info
        BankAccount bankAccount = reconciliation.getBankAccount();
        bankAccount.setLastReconciledDate(reconciliation.getStatementEndDate());
        bankAccount.setLastReconciledBalance(reconciliation.getStatementEndingBalance());
        bankAccountRepository.save(bankAccount);

        reconciliation.setStatus(ReconciliationStatus.COMPLETED);
        reconciliation.setCompletedAt(Instant.now());
        reconciliation.setCompletedBy(userId);
        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    @Transactional
    public BankReconciliationDto cancelReconciliation(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        BankReconciliation reconciliation = reconciliationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank reconciliation not found"));

        if (reconciliation.getStatus() == ReconciliationStatus.COMPLETED) {
            throw new BusinessException("Completed reconciliations cannot be cancelled");
        }

        // Unreconcile all transactions
        for (BankTransaction transaction : reconciliation.getReconciledTransactions()) {
            transaction.setStatus(BankTransactionStatus.CLEARED);
            transaction.setReconciliation(null);
            transaction.setReconciledDate(null);
            transactionRepository.save(transaction);
        }

        reconciliation.setStatus(ReconciliationStatus.CANCELLED);
        reconciliation.setCancelledAt(Instant.now());
        reconciliation.setCancelledBy(userId);
        reconciliation.setCancellationReason(reason);
        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    private void recalculateTotals(BankReconciliation reconciliation) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long bankAccountId = reconciliation.getBankAccount().getId();

        // Get cleared transactions for this reconciliation
        List<BankTransaction> clearedTransactions = transactionRepository.findByBankAccountIdAndStatusAndTenantId(
                bankAccountId, BankTransactionStatus.RECONCILED, tenantId);

        // Filter to only this reconciliation
        clearedTransactions = clearedTransactions.stream()
                .filter(t -> t.getReconciliation() != null && t.getReconciliation().getId().equals(reconciliation.getId()))
                .toList();

        BigDecimal clearedDeposits = BigDecimal.ZERO;
        BigDecimal clearedWithdrawals = BigDecimal.ZERO;

        for (BankTransaction tx : clearedTransactions) {
            clearedDeposits = clearedDeposits.add(tx.getCreditAmount());
            clearedWithdrawals = clearedWithdrawals.add(tx.getDebitAmount());
        }

        // Get outstanding transactions (pending or cleared but not reconciled)
        List<BankTransactionStatus> outstandingStatuses = Arrays.asList(
                BankTransactionStatus.PENDING, BankTransactionStatus.CLEARED);
        List<BankTransaction> outstandingTransactions = transactionRepository.findUnreconciledByBankAccountIdAndTenantId(
                bankAccountId, outstandingStatuses, reconciliation.getStatementEndDate(), tenantId);

        BigDecimal outstandingDeposits = BigDecimal.ZERO;
        BigDecimal outstandingWithdrawals = BigDecimal.ZERO;

        for (BankTransaction tx : outstandingTransactions) {
            outstandingDeposits = outstandingDeposits.add(tx.getCreditAmount());
            outstandingWithdrawals = outstandingWithdrawals.add(tx.getDebitAmount());
        }

        reconciliation.setClearedDeposits(clearedDeposits);
        reconciliation.setClearedWithdrawals(clearedWithdrawals);
        reconciliation.setOutstandingDeposits(outstandingDeposits);
        reconciliation.setOutstandingWithdrawals(outstandingWithdrawals);
        reconciliation.setTransactionsCleared(clearedTransactions.size());
        reconciliation.setTransactionsOutstanding(outstandingTransactions.size());

        // Calculate adjusted balances
        reconciliation.calculateAdjustedBankBalance();
        reconciliation.setAdjustedBookBalance(reconciliation.getBookBalance());
        reconciliation.calculateDifference();
    }

    private String generateReconciliationNumber(Long tenantId) {
        String maxNumber = reconciliationRepository.findMaxReconciliationNumberByTenantId(tenantId);
        if (maxNumber == null) {
            return "REC-000001";
        }
        int nextNumber = Integer.parseInt(maxNumber.substring(4)) + 1;
        return String.format("REC-%06d", nextNumber);
    }
}
