package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class BankAccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private BankAccount createBankAccount(Long tenantId, String accountCode, String accountName,
                                           BankAccountType accountType, String bankName,
                                           boolean active, boolean defaultAccount,
                                           boolean allowPayments, boolean allowReceipts,
                                           BigDecimal currentBalance) {
        BankAccount bankAccount = BankAccount.builder()
                .tenantId(tenantId)
                .accountCode(accountCode)
                .accountName(accountName)
                .accountType(accountType)
                .bankName(bankName)
                .active(active)
                .defaultAccount(defaultAccount)
                .allowPayments(allowPayments)
                .allowReceipts(allowReceipts)
                .currentBalance(currentBalance)
                .build();
        return entityManager.persistAndFlush(bankAccount);
    }

    // ---- findByAccountCodeAndTenantId ----

    @Test
    void findByAccountCodeAndTenantId_exists_returnsBankAccount() {
        createBankAccount(TENANT_ID, "BA-001", "Main Checking", BankAccountType.CHECKING,
                "National Bank", true, false, true, true, BigDecimal.valueOf(50000));

        Optional<BankAccount> result = bankAccountRepository.findByAccountCodeAndTenantId("BA-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Main Checking", result.get().getAccountName());
    }

    @Test
    void findByAccountCodeAndTenantId_wrongTenant_returnsEmpty() {
        createBankAccount(TENANT_ID, "BA-001", "Main Checking", BankAccountType.CHECKING,
                "National Bank", true, false, true, true, BigDecimal.valueOf(50000));

        Optional<BankAccount> result = bankAccountRepository.findByAccountCodeAndTenantId("BA-001", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findAllActiveByTenantId ----

    @Test
    void findAllActiveByTenantId_returnsActiveOnly() {
        createBankAccount(TENANT_ID, "BA-001", "Active Account", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.ZERO);
        createBankAccount(TENANT_ID, "BA-002", "Inactive Account", BankAccountType.CHECKING,
                "Bank B", false, false, true, true, BigDecimal.ZERO);

        List<BankAccount> result = bankAccountRepository.findAllActiveByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Active Account", result.get(0).getAccountName());
    }

    // ---- findByAccountTypeAndTenantId ----

    @Test
    void findByAccountTypeAndTenantId_returnsMatchingType() {
        createBankAccount(TENANT_ID, "BA-001", "Checking", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.ZERO);
        createBankAccount(TENANT_ID, "BA-002", "Savings", BankAccountType.SAVINGS,
                "Bank B", true, false, true, true, BigDecimal.ZERO);

        List<BankAccount> result = bankAccountRepository.findByAccountTypeAndTenantId(
                BankAccountType.CHECKING, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Checking", result.get(0).getAccountName());
    }

    // ---- findDefaultAccountByTenantId ----

    @Test
    void findDefaultAccountByTenantId_returnsDefault() {
        createBankAccount(TENANT_ID, "BA-001", "Non-Default", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.ZERO);
        createBankAccount(TENANT_ID, "BA-002", "Default Account", BankAccountType.CHECKING,
                "Bank B", true, true, true, true, BigDecimal.ZERO);

        Optional<BankAccount> result = bankAccountRepository.findDefaultAccountByTenantId(TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Default Account", result.get().getAccountName());
    }

    @Test
    void findDefaultAccountByTenantId_noDefault_returnsEmpty() {
        createBankAccount(TENANT_ID, "BA-001", "Non-Default", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.ZERO);

        Optional<BankAccount> result = bankAccountRepository.findDefaultAccountByTenantId(TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findPaymentAccountsByTenantId ----

    @Test
    void findPaymentAccountsByTenantId_returnsPaymentAccounts() {
        createBankAccount(TENANT_ID, "BA-001", "Payment Account", BankAccountType.CHECKING,
                "Bank A", true, false, true, false, BigDecimal.ZERO);
        createBankAccount(TENANT_ID, "BA-002", "No Payment", BankAccountType.CHECKING,
                "Bank B", true, false, false, true, BigDecimal.ZERO);

        List<BankAccount> result = bankAccountRepository.findPaymentAccountsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Payment Account", result.get(0).getAccountName());
    }

    // ---- findReceiptAccountsByTenantId ----

    @Test
    void findReceiptAccountsByTenantId_returnsReceiptAccounts() {
        createBankAccount(TENANT_ID, "BA-001", "Receipt Account", BankAccountType.CHECKING,
                "Bank A", true, false, false, true, BigDecimal.ZERO);
        createBankAccount(TENANT_ID, "BA-002", "No Receipt", BankAccountType.CHECKING,
                "Bank B", true, false, true, false, BigDecimal.ZERO);

        List<BankAccount> result = bankAccountRepository.findReceiptAccountsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Receipt Account", result.get(0).getAccountName());
    }

    // ---- findByGlAccountIdAndTenantId ----

    @Test
    void findByGlAccountIdAndTenantId_returnsMatchingAccount() {
        Account glAccount = Account.builder()
                .tenantId(TENANT_ID)
                .code("1010")
                .name("Cash GL")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .build();
        glAccount = entityManager.persistAndFlush(glAccount);

        BankAccount ba = BankAccount.builder()
                .tenantId(TENANT_ID)
                .accountCode("BA-001")
                .accountName("Bank Account")
                .accountType(BankAccountType.CHECKING)
                .glAccount(glAccount)
                .build();
        entityManager.persistAndFlush(ba);

        Optional<BankAccount> result = bankAccountRepository.findByGlAccountIdAndTenantId(
                glAccount.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("BA-001", result.get().getAccountCode());
    }

    // ---- existsByAccountCodeAndTenantId ----

    @Test
    void existsByAccountCodeAndTenantId_exists_returnsTrue() {
        createBankAccount(TENANT_ID, "BA-001", "Test Account", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.ZERO);

        assertTrue(bankAccountRepository.existsByAccountCodeAndTenantId("BA-001", TENANT_ID));
    }

    @Test
    void existsByAccountCodeAndTenantId_notExists_returnsFalse() {
        assertFalse(bankAccountRepository.existsByAccountCodeAndTenantId("BA-999", TENANT_ID));
    }

    // ---- searchByTenantId ----

    @Test
    void searchByTenantId_byAccountName_returnsMatching() {
        createBankAccount(TENANT_ID, "BA-001", "Main Checking", BankAccountType.CHECKING,
                "National Bank", true, false, true, true, BigDecimal.ZERO);
        createBankAccount(TENANT_ID, "BA-002", "Petty Cash", BankAccountType.PETTY_CASH,
                "Internal", true, false, true, true, BigDecimal.ZERO);

        Page<BankAccount> result = bankAccountRepository.searchByTenantId(
                "checking", TENANT_ID, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchByTenantId_byBankName_returnsMatching() {
        createBankAccount(TENANT_ID, "BA-001", "Main Account", BankAccountType.CHECKING,
                "National Bank", true, false, true, true, BigDecimal.ZERO);

        Page<BankAccount> result = bankAccountRepository.searchByTenantId(
                "national", TENANT_ID, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    // ---- sumTotalBalance ----

    @Test
    void sumTotalBalance_returnsSum() {
        createBankAccount(TENANT_ID, "BA-001", "Account 1", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.valueOf(10000));
        createBankAccount(TENANT_ID, "BA-002", "Account 2", BankAccountType.SAVINGS,
                "Bank B", true, false, true, true, BigDecimal.valueOf(20000));
        createBankAccount(TENANT_ID, "BA-003", "Inactive", BankAccountType.CHECKING,
                "Bank C", false, false, true, true, BigDecimal.valueOf(5000));

        BigDecimal result = bankAccountRepository.sumTotalBalance(TENANT_ID);

        assertEquals(0, BigDecimal.valueOf(30000).compareTo(result));
    }

    // ---- sumBalanceByTypes ----

    @Test
    void sumBalanceByTypes_returnsFilteredSum() {
        createBankAccount(TENANT_ID, "BA-001", "Checking", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.valueOf(10000));
        createBankAccount(TENANT_ID, "BA-002", "Savings", BankAccountType.SAVINGS,
                "Bank B", true, false, true, true, BigDecimal.valueOf(20000));
        createBankAccount(TENANT_ID, "BA-003", "Credit Card", BankAccountType.CREDIT_CARD,
                "Bank C", true, false, true, true, BigDecimal.valueOf(5000));

        BigDecimal result = bankAccountRepository.sumBalanceByTypes(
                TENANT_ID, List.of(BankAccountType.CHECKING, BankAccountType.SAVINGS));

        assertEquals(0, BigDecimal.valueOf(30000).compareTo(result));
    }

    // ---- sumBalanceExcludingTypes ----

    @Test
    void sumBalanceExcludingTypes_returnsFilteredSum() {
        createBankAccount(TENANT_ID, "BA-001", "Checking", BankAccountType.CHECKING,
                "Bank A", true, false, true, true, BigDecimal.valueOf(10000));
        createBankAccount(TENANT_ID, "BA-002", "Credit Card", BankAccountType.CREDIT_CARD,
                "Bank B", true, false, true, true, BigDecimal.valueOf(5000));

        BigDecimal result = bankAccountRepository.sumBalanceExcludingTypes(
                TENANT_ID, List.of(BankAccountType.CREDIT_CARD));

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(result));
    }
}
