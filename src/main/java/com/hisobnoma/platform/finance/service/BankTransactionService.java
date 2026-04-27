package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankTransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final BankTransactionMapper bankTransactionMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<BankTransactionDto> getTransactions(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<BankTransaction> page = bankTransactionRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(bankTransactionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<BankTransactionDto> getTransactionsByBankAccount(Long bankAccountId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<BankTransaction> page = bankTransactionRepository.findByBankAccountIdAndTenantId(bankAccountId, tenantId, pageable);
        return PageResponse.of(page.map(bankTransactionMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<BankTransactionDto> getTransactionsByDateRange(Long bankAccountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankTransaction> transactions = bankTransactionRepository.findByBankAccountIdAndDateRangeAndTenantId(
                bankAccountId, startDate, endDate, tenantId);
        return bankTransactionMapper.toDtoList(transactions);
    }

    @Transactional(readOnly = true)
    public BankTransactionDto getTransaction(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankTransaction transaction = bankTransactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank transaction not found with id: " + id));
        return bankTransactionMapper.toDto(transaction);
    }

    @Transactional
    public BankTransactionDto createTransaction(CreateBankTransactionRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankAccount bankAccount = bankAccountRepository.findByIdAndTenantId(request.getBankAccountId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + request.getBankAccountId()));

        if (!bankAccount.isActive()) {
            throw new BusinessException("Cannot create transaction for inactive bank account");
        }

        BankTransaction transaction = bankTransactionMapper.toEntity(request);
        transaction.setTenantId(tenantId);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionNumber(generateTransactionNumber(tenantId));
        transaction.setStatus(BankTransactionStatus.PENDING);

        // Set debit/credit amounts based on transaction type and isDebit flag
        BigDecimal amount = request.getAmount();
        if (request.isDebit()) {
            transaction.setDebitAmount(amount);
            transaction.setCreditAmount(BigDecimal.ZERO);
        } else {
            transaction.setDebitAmount(BigDecimal.ZERO);
            transaction.setCreditAmount(amount);
        }

        // Set exchange rate and base amount
        if (request.getExchangeRate() != null) {
            transaction.setExchangeRate(request.getExchangeRate());
            transaction.setBaseAmount(amount.multiply(request.getExchangeRate()));
        } else {
            transaction.setExchangeRate(BigDecimal.ONE);
            transaction.setBaseAmount(amount);
        }

        // Link counter account if provided
        if (request.getCounterAccountId() != null) {
            Account counterAccount = accountRepository.findByIdAndTenantId(request.getCounterAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Counter account not found with id: " + request.getCounterAccountId()));
            transaction.setCounterAccount(counterAccount);
        }

        // Link transfer account if this is a transfer
        if (request.getTransactionType() == BankTransactionType.TRANSFER_OUT && request.getTransferToAccountId() != null) {
            BankAccount transferToAccount = bankAccountRepository.findByIdAndTenantId(request.getTransferToAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Transfer target account not found with id: " + request.getTransferToAccountId()));
            transaction.setTransferToAccount(transferToAccount);
        }

        // Calculate running balance
        BigDecimal netChange = transaction.getCreditAmount().subtract(transaction.getDebitAmount());
        transaction.setRunningBalance(bankAccount.getCurrentBalance().add(netChange));

        transaction = bankTransactionRepository.save(transaction);

        // Update bank account balance
        bankAccount.updateBalance(netChange);
        bankAccountRepository.save(bankAccount);

        return bankTransactionMapper.toDto(transaction);
    }

    @Transactional
    public BankTransactionDto createTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                              LocalDate transactionDate, String description) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankAccount fromAccount = bankAccountRepository.findByIdAndTenantId(fromAccountId, tenantId)
                .orElseThrow(() -> new NotFoundException("Source bank account not found"));
        BankAccount toAccount = bankAccountRepository.findByIdAndTenantId(toAccountId, tenantId)
                .orElseThrow(() -> new NotFoundException("Target bank account not found"));

        if (!fromAccount.isActive() || !toAccount.isActive()) {
            throw new BusinessException("Cannot transfer between inactive accounts");
        }

        // Create and save outgoing transaction first to reserve its transaction number
        BankTransaction outgoing = BankTransaction.builder()
                .bankAccount(fromAccount)
                .transactionNumber(generateTransactionNumber(tenantId))
                .transactionDate(transactionDate)
                .transactionType(BankTransactionType.TRANSFER_OUT)
                .status(BankTransactionStatus.CLEARED)
                .description(description != null ? description : "Transfer to " + toAccount.getAccountName())
                .debitAmount(amount)
                .creditAmount(BigDecimal.ZERO)
                .currency(fromAccount.getCurrency())
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(amount)
                .transferToAccount(toAccount)
                .runningBalance(fromAccount.getCurrentBalance().subtract(amount))
                .tenantId(tenantId)
                .build();
        outgoing = bankTransactionRepository.saveAndFlush(outgoing);

        // Create incoming transaction after outgoing is persisted
        BankTransaction incoming = BankTransaction.builder()
                .bankAccount(toAccount)
                .transactionNumber(generateTransactionNumber(tenantId))
                .transactionDate(transactionDate)
                .transactionType(BankTransactionType.TRANSFER_IN)
                .status(BankTransactionStatus.CLEARED)
                .description(description != null ? description : "Transfer from " + fromAccount.getAccountName())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(amount)
                .currency(toAccount.getCurrency())
                .exchangeRate(BigDecimal.ONE)
                .baseAmount(amount)
                .runningBalance(toAccount.getCurrentBalance().add(amount))
                .tenantId(tenantId)
                .build();
        bankTransactionRepository.save(incoming);

        // Update account balances
        fromAccount.updateBalance(amount.negate());
        toAccount.updateBalance(amount);
        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        return bankTransactionMapper.toDto(outgoing);
    }

    @Transactional
    public BankTransactionDto clearTransaction(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankTransaction transaction = bankTransactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank transaction not found with id: " + id));

        if (transaction.getStatus() != BankTransactionStatus.PENDING) {
            throw new BusinessException("Only pending transactions can be cleared");
        }

        transaction.setStatus(BankTransactionStatus.CLEARED);
        transaction = bankTransactionRepository.save(transaction);
        return bankTransactionMapper.toDto(transaction);
    }

    @Transactional
    public BankTransactionDto voidTransaction(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankTransaction transaction = bankTransactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank transaction not found with id: " + id));

        if (transaction.getStatus() == BankTransactionStatus.RECONCILED) {
            throw new BusinessException("Reconciled transactions cannot be voided");
        }

        if (transaction.getStatus() == BankTransactionStatus.VOIDED) {
            throw new BusinessException("Transaction is already voided");
        }

        // Reverse the balance change
        BigDecimal netChange = transaction.getCreditAmount().subtract(transaction.getDebitAmount());
        BankAccount bankAccount = transaction.getBankAccount();
        bankAccount.updateBalance(netChange.negate());
        bankAccountRepository.save(bankAccount);

        transaction.setStatus(BankTransactionStatus.VOIDED);
        if (reason != null) {
            transaction.setNotes((transaction.getNotes() != null ? transaction.getNotes() + "\n" : "") + "Voided: " + reason);
        }
        transaction = bankTransactionRepository.save(transaction);
        return bankTransactionMapper.toDto(transaction);
    }

    @Transactional(readOnly = true)
    public CashFlowDto getCashFlow(Long bankAccountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankAccount bankAccount = bankAccountRepository.findByIdAndTenantId(bankAccountId, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found"));

        List<BankTransaction> transactions = bankTransactionRepository.findByBankAccountIdAndDateRangeAndTenantId(
                bankAccountId, startDate, endDate, tenantId);

        // Calculate opening balance (balance as of start date - transactions in period)
        BigDecimal periodInflows = BigDecimal.ZERO;
        BigDecimal periodOutflows = BigDecimal.ZERO;

        for (BankTransaction tx : transactions) {
            if (tx.getStatus() != BankTransactionStatus.VOIDED) {
                periodInflows = periodInflows.add(tx.getCreditAmount());
                periodOutflows = periodOutflows.add(tx.getDebitAmount());
            }
        }

        BigDecimal closingBalance = bankAccount.getCurrentBalance();
        BigDecimal openingBalance = closingBalance.subtract(periodInflows).add(periodOutflows);
        BigDecimal netCashFlow = periodInflows.subtract(periodOutflows);

        // Group by transaction type for details
        Map<BankTransactionType, List<BankTransaction>> inflowsByType = transactions.stream()
                .filter(tx -> tx.getCreditAmount().compareTo(BigDecimal.ZERO) > 0 && tx.getStatus() != BankTransactionStatus.VOIDED)
                .collect(Collectors.groupingBy(BankTransaction::getTransactionType));

        Map<BankTransactionType, List<BankTransaction>> outflowsByType = transactions.stream()
                .filter(tx -> tx.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 && tx.getStatus() != BankTransactionStatus.VOIDED)
                .collect(Collectors.groupingBy(BankTransaction::getTransactionType));

        List<CashFlowDto.CashFlowLineDto> inflowDetails = new ArrayList<>();
        for (Map.Entry<BankTransactionType, List<BankTransaction>> entry : inflowsByType.entrySet()) {
            BigDecimal sum = entry.getValue().stream()
                    .map(BankTransaction::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            inflowDetails.add(CashFlowDto.CashFlowLineDto.builder()
                    .category(entry.getKey().name())
                    .description(entry.getKey().name())
                    .amount(sum)
                    .transactionCount(entry.getValue().size())
                    .build());
        }

        List<CashFlowDto.CashFlowLineDto> outflowDetails = new ArrayList<>();
        for (Map.Entry<BankTransactionType, List<BankTransaction>> entry : outflowsByType.entrySet()) {
            BigDecimal sum = entry.getValue().stream()
                    .map(BankTransaction::getDebitAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            outflowDetails.add(CashFlowDto.CashFlowLineDto.builder()
                    .category(entry.getKey().name())
                    .description(entry.getKey().name())
                    .amount(sum)
                    .transactionCount(entry.getValue().size())
                    .build());
        }

        return CashFlowDto.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .openingBalance(openingBalance)
                .totalInflows(periodInflows)
                .totalOutflows(periodOutflows)
                .netCashFlow(netCashFlow)
                .closingBalance(closingBalance)
                .inflowDetails(inflowDetails)
                .outflowDetails(outflowDetails)
                .build();
    }

    private String generateTransactionNumber(Long tenantId) {
        String maxNumber = bankTransactionRepository.findMaxTransactionNumberByTenantId(tenantId);
        if (maxNumber == null) {
            return "BT-000001";
        }
        int nextNumber = Integer.parseInt(maxNumber.substring(3)) + 1;
        return String.format("BT-%06d", nextNumber);
    }
}
