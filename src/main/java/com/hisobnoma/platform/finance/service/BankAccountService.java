package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final BankAccountMapper bankAccountMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<BankAccountDto> getBankAccounts(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<BankAccount> page = bankAccountRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(bankAccountMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> getAllBankAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankAccount> accounts = bankAccountRepository.findAllByTenantId(tenantId);
        return bankAccountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> getActiveBankAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankAccount> accounts = bankAccountRepository.findAllActiveByTenantId(tenantId);
        return bankAccountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> getBankAccountsByType(BankAccountType type) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankAccount> accounts = bankAccountRepository.findByAccountTypeAndTenantId(type, tenantId);
        return bankAccountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> getPaymentAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankAccount> accounts = bankAccountRepository.findPaymentAccountsByTenantId(tenantId);
        return bankAccountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> getReceiptAccounts() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<BankAccount> accounts = bankAccountRepository.findReceiptAccountsByTenantId(tenantId);
        return bankAccountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public BankAccountDto getBankAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankAccount account = bankAccountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + id));
        return bankAccountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public BankAccountDto getBankAccountByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankAccount account = bankAccountRepository.findByAccountCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with code: " + code));
        return bankAccountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public BankAccountDto getDefaultBankAccount() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankAccount account = bankAccountRepository.findDefaultAccountByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("No default bank account configured"));
        return bankAccountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public PageResponse<BankAccountDto> searchBankAccounts(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<BankAccount> page = bankAccountRepository.searchByTenantId(search, tenantId, pageable);
        return PageResponse.of(page.map(bankAccountMapper::toDto));
    }

    @Transactional
    public BankAccountDto createBankAccount(CreateBankAccountRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check if code already exists
        if (bankAccountRepository.existsByAccountCodeAndTenantId(request.getAccountCode(), tenantId)) {
            throw new BusinessException("Bank account with code '" + request.getAccountCode() + "' already exists");
        }

        BankAccount account = bankAccountMapper.toEntity(request);
        account.setTenantId(tenantId);
        account.setActive(true);

        // Set opening balance
        if (request.getOpeningBalance() != null) {
            account.setOpeningBalance(request.getOpeningBalance());
            account.setCurrentBalance(request.getOpeningBalance());
            account.setAvailableBalance(request.getOpeningBalance());
        } else {
            account.setOpeningBalance(BigDecimal.ZERO);
            account.setCurrentBalance(BigDecimal.ZERO);
            account.setAvailableBalance(BigDecimal.ZERO);
        }

        // Link to GL account if provided
        if (request.getGlAccountId() != null) {
            Account glAccount = accountRepository.findByIdAndTenantId(request.getGlAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("GL account not found with id: " + request.getGlAccountId()));
            account.setGlAccount(glAccount);
        }

        // If this is set as default, unset any existing default
        if (request.isDefaultAccount()) {
            bankAccountRepository.findDefaultAccountByTenantId(tenantId)
                    .ifPresent(defaultAccount -> {
                        defaultAccount.setDefaultAccount(false);
                        bankAccountRepository.save(defaultAccount);
                    });
        }

        account = bankAccountRepository.save(account);
        return bankAccountMapper.toDto(account);
    }

    @Transactional
    public BankAccountDto updateBankAccount(Long id, CreateBankAccountRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankAccount account = bankAccountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + id));

        // Check for duplicate code if changed
        if (!account.getAccountCode().equals(request.getAccountCode())) {
            if (bankAccountRepository.existsByAccountCodeAndTenantId(request.getAccountCode(), tenantId)) {
                throw new BusinessException("Bank account with code '" + request.getAccountCode() + "' already exists");
            }
        }

        bankAccountMapper.updateEntity(request, account);

        // Update GL account link
        if (request.getGlAccountId() != null) {
            Account glAccount = accountRepository.findByIdAndTenantId(request.getGlAccountId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("GL account not found with id: " + request.getGlAccountId()));
            account.setGlAccount(glAccount);
        } else {
            account.setGlAccount(null);
        }

        // Handle default account change
        if (request.isDefaultAccount() && !account.isDefaultAccount()) {
            final Long accountId = account.getId();
            bankAccountRepository.findDefaultAccountByTenantId(tenantId)
                    .ifPresent(defaultAccount -> {
                        if (!defaultAccount.getId().equals(accountId)) {
                            defaultAccount.setDefaultAccount(false);
                            bankAccountRepository.save(defaultAccount);
                        }
                    });
            account.setDefaultAccount(true);
        }

        account = bankAccountRepository.save(account);
        return bankAccountMapper.toDto(account);
    }

    @Transactional
    public BankAccountDto activateBankAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankAccount account = bankAccountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + id));

        account.setActive(true);
        account = bankAccountRepository.save(account);
        return bankAccountMapper.toDto(account);
    }

    @Transactional
    public BankAccountDto deactivateBankAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        BankAccount account = bankAccountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + id));

        if (account.isDefaultAccount()) {
            throw new BusinessException("Default bank account cannot be deactivated");
        }

        if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Cannot deactivate account with non-zero balance");
        }

        account.setActive(false);
        account = bankAccountRepository.save(account);
        return bankAccountMapper.toDto(account);
    }

    @Transactional
    public BankAccountDto setDefaultBankAccount(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Unset any existing default
        bankAccountRepository.findDefaultAccountByTenantId(tenantId)
                .ifPresent(defaultAccount -> {
                    defaultAccount.setDefaultAccount(false);
                    bankAccountRepository.save(defaultAccount);
                });

        // Set the new default
        BankAccount account = bankAccountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + id));

        account.setDefaultAccount(true);
        account.setActive(true);
        account = bankAccountRepository.save(account);
        return bankAccountMapper.toDto(account);
    }

    @Transactional
    public void updateBalance(Long bankAccountId, BigDecimal amount) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        BankAccount account = bankAccountRepository.findByIdAndTenantId(bankAccountId, tenantId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with id: " + bankAccountId));
        account.updateBalance(amount);
        bankAccountRepository.save(account);
    }
}
