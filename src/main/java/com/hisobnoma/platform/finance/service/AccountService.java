package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.AccountDto;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.mapper.AccountMapper;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<AccountDto> getAccounts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Account> page = accountRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(accountMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Account> accounts = accountRepository.findAllByTenantId(tenantId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getRootAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Account> accounts = accountRepository.findRootAccountsByTenantId(tenantId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getChildAccounts(Long parentId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Account> accounts = accountRepository.findChildAccountsByParentId(tenantId, parentId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountWithChildren(Long id) {
        Account account = accountRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));
        return accountMapper.toDtoWithChildren(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Account account = accountRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with code: " + code));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByType(AccountType accountType) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Account> accounts = accountRepository.findByAccountTypeAndTenantId(tenantId, accountType);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getActiveAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Account> accounts = accountRepository.findAllActiveByTenantId(tenantId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getPostableAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Account> accounts = accountRepository.findAllPostableByTenantId(tenantId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountDto> searchAccounts(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Account> page = accountRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(accountMapper::toDto));
    }

    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (accountRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Account with code '" + request.getCode() + "' already exists");
        }

        Account account = accountMapper.toEntity(request);
        account.setTenantId(tenantId);
        account.setActive(true);
        account.setNormalBalance(Account.determineNormalBalance(request.getAccountType()));

        // Handle parent account
        if (request.getParentAccountId() != null) {
            Account parent = accountRepository.findByIdAndTenantId(request.getParentAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Parent account not found with id: " + request.getParentAccountId()));

            // Validate parent account type matches
            if (parent.getAccountType() != request.getAccountType()) {
                throw new BusinessException("Parent account type must match child account type");
            }

            parent.addChildAccount(account);
        } else {
            account.setAccountLevel(1);
            account.setFullPath(request.getCode());
        }

        // Set opening balance
        if (request.getOpeningBalance() != null) {
            account.setOpeningBalance(request.getOpeningBalance());
            account.setCurrentBalance(request.getOpeningBalance());
        }

        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Transactional
    public AccountDto updateAccount(Long id, CreateAccountRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));

        if (account.isSystemAccount()) {
            throw new BusinessException("System accounts cannot be modified");
        }

        // Check for duplicate code if changed
        if (!account.getCode().equals(request.getCode()) &&
            accountRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Account with code '" + request.getCode() + "' already exists");
        }

        // Cannot change account type if there are postings
        if (account.getAccountType() != request.getAccountType() &&
            (account.getYtdDebit().compareTo(BigDecimal.ZERO) != 0 ||
             account.getYtdCredit().compareTo(BigDecimal.ZERO) != 0)) {
            throw new BusinessException("Cannot change account type for an account with existing transactions");
        }

        accountMapper.updateEntity(request, account);
        account.setNormalBalance(Account.determineNormalBalance(request.getAccountType()));

        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));

        if (account.isSystemAccount()) {
            throw new BusinessException("System accounts cannot be deleted");
        }

        if (!account.isLeafAccount()) {
            throw new BusinessException("Cannot delete account with child accounts");
        }

        if (account.getYtdDebit().compareTo(BigDecimal.ZERO) != 0 ||
            account.getYtdCredit().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Cannot delete account with existing transactions. Deactivate instead.");
        }

        accountRepository.delete(account);
    }

    @Transactional
    public AccountDto activateAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));

        account.setActive(true);
        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    @Transactional
    public AccountDto deactivateAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));

        if (account.isSystemAccount()) {
            throw new BusinessException("System accounts cannot be deactivated");
        }

        // Check if account has active children
        if (!account.isLeafAccount()) {
            boolean hasActiveChildren = account.getChildAccounts().stream()
                    .anyMatch(Account::isActive);
            if (hasActiveChildren) {
                throw new BusinessException("Cannot deactivate account with active child accounts");
            }
        }

        account.setActive(false);
        account = accountRepository.save(account);
        return accountMapper.toDto(account);
    }

    /**
     * Internal method to get an account entity by ID.
     */
    @Transactional(readOnly = true)
    public Account getAccountEntity(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + id));
    }

    /**
     * Updates account balances after posting a journal entry.
     */
    @Transactional
    public void updateAccountBalance(Long accountId, BigDecimal debitAmount, BigDecimal creditAmount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        account.setYtdDebit(account.getYtdDebit().add(debitAmount));
        account.setYtdCredit(account.getYtdCredit().add(creditAmount));
        account.setCurrentBalance(account.getBalance());

        accountRepository.save(account);
    }
}
