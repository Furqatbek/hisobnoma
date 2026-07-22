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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Account account = accountRepository.findByIdWithChildren(id, tenantId)
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
     * Updates account balances after posting a journal entry. Takes the
     * already-loaded (tenant-validated) account so no unscoped re-fetch
     * by id is needed.
     */
    @Transactional
    public void updateAccountBalance(Account account, BigDecimal debitAmount, BigDecimal creditAmount) {
        account.setYtdDebit(account.getYtdDebit().add(debitAmount));
        account.setYtdCredit(account.getYtdCredit().add(creditAmount));
        account.setCurrentBalance(account.getBalance());

        accountRepository.save(account);
    }

    // ==================== Import Methods ====================

    /**
     * Import chart of accounts from CSV file.
     * CSV format: code,name,accountType,description,parentCode
     */
    @Transactional
    public ImportResult importFromCsv(MultipartFile file) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;
            boolean headerProcessed = false;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (!headerProcessed) {
                    headerProcessed = true;
                    continue;
                }

                try {
                    String[] values = parseCsvLine(line);
                    if (values.length < 3) {
                        result.addError(lineNum, "Invalid row format - minimum 3 columns required");
                        continue;
                    }

                    String code = values[0].trim();
                    String name = values[1].trim();
                    String typeStr = values[2].trim();
                    String description = values.length > 3 ? values[3].trim() : null;
                    String parentCode = values.length > 4 ? values[4].trim() : null;

                    if (code.isEmpty() || name.isEmpty() || typeStr.isEmpty()) {
                        result.addError(lineNum, "Code, name, and type are required");
                        continue;
                    }

                    AccountType accountType;
                    try {
                        accountType = AccountType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        result.addError(lineNum, "Invalid account type: " + typeStr);
                        continue;
                    }

                    // Skip if account already exists
                    if (accountRepository.existsByCodeAndTenantId(code, tenantId)) {
                        result.addSkipped(lineNum, "Account with code '" + code + "' already exists");
                        continue;
                    }

                    Account account = Account.builder()
                            .tenantId(tenantId)
                            .code(code)
                            .name(name)
                            .accountType(accountType)
                            .description(description != null && !description.isEmpty() ? description : null)
                            .normalBalance(Account.determineNormalBalance(accountType))
                            .active(true)
                            .allowsDirectPosting(true)
                            .ytdDebit(BigDecimal.ZERO)
                            .ytdCredit(BigDecimal.ZERO)
                            .openingBalance(BigDecimal.ZERO)
                            .currentBalance(BigDecimal.ZERO)
                            .build();

                    // Handle parent account
                    if (parentCode != null && !parentCode.isEmpty()) {
                        accountRepository.findByCodeAndTenantId(parentCode, tenantId)
                                .ifPresent(parent -> parent.addChildAccount(account));
                    } else {
                        account.setAccountLevel(1);
                        account.setFullPath(code);
                    }

                    accountRepository.save(account);
                    result.incrementSuccess();

                } catch (Exception e) {
                    result.addError(lineNum, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to import chart of accounts: {}", e.getMessage());
            throw new BusinessException("Failed to import: " + e.getMessage());
        }

        log.info("Import completed: {} success, {} errors, {} skipped",
                result.successCount, result.errors.size(), result.skipped.size());

        return result;
    }

    /**
     * Generate a default chart of accounts for Uzbekistan SME.
     */
    @Transactional
    public List<AccountDto> generateDefaultChartOfAccounts() {
        return generateDefaultChartOfAccounts(securityContextHelper.getCurrentTenantId());
    }

    /** Tenant-parameterized variant for provisioning flows that run outside the tenant's own context. */
    public List<AccountDto> generateDefaultChartOfAccounts(Long tenantId) {
        // Check if accounts already exist
        if (accountRepository.countByTenantId(tenantId) > 0) {
            throw new BusinessException("Cannot generate default accounts - accounts already exist");
        }

        List<Account> accounts = new ArrayList<>();

        // Assets (1xxx)
        accounts.add(createAccount(tenantId, "1000", "Assets", AccountType.ASSET, null, false));
        accounts.add(createAccount(tenantId, "1100", "Current Assets", AccountType.ASSET, "1000", false));
        accounts.add(createAccount(tenantId, "1110", "Cash", AccountType.ASSET, "1100", true));
        accounts.add(createAccount(tenantId, "1120", "Bank Accounts", AccountType.ASSET, "1100", true));
        accounts.add(createAccount(tenantId, "1130", "Accounts Receivable", AccountType.ASSET, "1100", true));
        accounts.add(createAccount(tenantId, "1140", "Inventory", AccountType.ASSET, "1100", true));
        accounts.add(createAccount(tenantId, "1150", "Prepaid Expenses", AccountType.ASSET, "1100", true));
        accounts.add(createAccount(tenantId, "1200", "Fixed Assets", AccountType.ASSET, "1000", false));
        accounts.add(createAccount(tenantId, "1210", "Equipment", AccountType.ASSET, "1200", true));
        accounts.add(createAccount(tenantId, "1220", "Vehicles", AccountType.ASSET, "1200", true));
        accounts.add(createAccount(tenantId, "1230", "Furniture", AccountType.ASSET, "1200", true));
        accounts.add(createAccount(tenantId, "1290", "Accumulated Depreciation", AccountType.ASSET, "1200", true));

        // Liabilities (2xxx)
        accounts.add(createAccount(tenantId, "2000", "Liabilities", AccountType.LIABILITY, null, false));
        accounts.add(createAccount(tenantId, "2100", "Current Liabilities", AccountType.LIABILITY, "2000", false));
        accounts.add(createAccount(tenantId, "2110", "Accounts Payable", AccountType.LIABILITY, "2100", true));
        accounts.add(createAccount(tenantId, "2120", "Accrued Expenses", AccountType.LIABILITY, "2100", true));
        accounts.add(createAccount(tenantId, "2130", "VAT Payable", AccountType.LIABILITY, "2100", true));
        accounts.add(createAccount(tenantId, "2140", "Salaries Payable", AccountType.LIABILITY, "2100", true));
        accounts.add(createAccount(tenantId, "2200", "Long-term Liabilities", AccountType.LIABILITY, "2000", false));
        accounts.add(createAccount(tenantId, "2210", "Bank Loans", AccountType.LIABILITY, "2200", true));

        // Equity (3xxx)
        accounts.add(createAccount(tenantId, "3000", "Equity", AccountType.EQUITY, null, false));
        accounts.add(createAccount(tenantId, "3100", "Share Capital", AccountType.EQUITY, "3000", true));
        accounts.add(createAccount(tenantId, "3200", "Retained Earnings", AccountType.EQUITY, "3000", true));
        accounts.add(createAccount(tenantId, "3300", "Current Year Earnings", AccountType.EQUITY, "3000", true));

        // Revenue (4xxx)
        accounts.add(createAccount(tenantId, "4000", "Revenue", AccountType.REVENUE, null, false));
        accounts.add(createAccount(tenantId, "4100", "Sales Revenue", AccountType.REVENUE, "4000", true));
        accounts.add(createAccount(tenantId, "4110", "Service Revenue", AccountType.REVENUE, "4000", true));
        // 4200/4300 are resolved by code in GLIntegrationService (sales discounts debited,
        // purchase discounts credited) — names/types must match the migration-seeded chart.
        accounts.add(createAccount(tenantId, "4200", "Sales Discounts", AccountType.EXPENSE, "4000", true));
        accounts.add(createAccount(tenantId, "4300", "Purchase Discounts", AccountType.REVENUE, "4000", true));
        accounts.add(createAccount(tenantId, "4400", "Other Revenue", AccountType.REVENUE, "4000", true));
        accounts.add(createAccount(tenantId, "4900", "Sales Returns", AccountType.REVENUE, "4000", true));

        // Expenses (5xxx)
        accounts.add(createAccount(tenantId, "5000", "Expenses", AccountType.EXPENSE, null, false));
        accounts.add(createAccount(tenantId, "5100", "Cost of Goods Sold", AccountType.EXPENSE, "5000", true));
        // GL posts purchases directly to 5200 (PURCHASE_EXPENSE) — it must allow direct posting.
        accounts.add(createAccount(tenantId, "5200", "Purchase Expenses", AccountType.EXPENSE, "5000", true));
        accounts.add(createAccount(tenantId, "5210", "Salaries Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5220", "Rent Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5230", "Utilities Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5240", "Office Supplies", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5250", "Depreciation Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5260", "Insurance Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5270", "Marketing Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5280", "Travel Expense", AccountType.EXPENSE, "5200", true));
        accounts.add(createAccount(tenantId, "5290", "Miscellaneous Expense", AccountType.EXPENSE, "5200", true));

        // Save all accounts
        for (Account account : accounts) {
            accountRepository.save(account);
        }

        log.info("Generated {} default accounts for tenant {}", accounts.size(), tenantId);

        return accountMapper.toDtoList(accounts);
    }

    private Account createAccount(Long tenantId, String code, String name, AccountType type,
                                   String parentCode, boolean allowsPosting) {
        Account account = Account.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .accountType(type)
                .normalBalance(Account.determineNormalBalance(type))
                .active(true)
                .allowsDirectPosting(allowsPosting)
                .ytdDebit(BigDecimal.ZERO)
                .ytdCredit(BigDecimal.ZERO)
                .openingBalance(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .build();

        if (parentCode != null) {
            accountRepository.findByCodeAndTenantId(parentCode, tenantId)
                    .ifPresent(parent -> parent.addChildAccount(account));
        } else {
            account.setAccountLevel(1);
            account.setFullPath(code);
        }

        return account;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());

        return values.toArray(new String[0]);
    }

    /**
     * Result of import operation.
     */
    public static class ImportResult {
        public int successCount = 0;
        public List<String> errors = new ArrayList<>();
        public List<String> skipped = new ArrayList<>();

        public void incrementSuccess() {
            successCount++;
        }

        public void addError(int line, String message) {
            errors.add("Line " + line + ": " + message);
        }

        public void addSkipped(int line, String message) {
            skipped.add("Line " + line + ": " + message);
        }
    }
}
