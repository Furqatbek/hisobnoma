package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import com.hisobnoma.platform.finance.entity.NormalBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private Account createAccount(Long tenantId, String code, String name,
                                   AccountType accountType, boolean active,
                                   boolean controlAccount, boolean systemAccount,
                                   boolean allowsDirectPosting) {
        Account account = Account.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .accountType(accountType)
                .normalBalance(Account.determineNormalBalance(accountType))
                .active(active)
                .controlAccount(controlAccount)
                .systemAccount(systemAccount)
                .allowsDirectPosting(allowsDirectPosting)
                .build();
        return entityManager.persistAndFlush(account);
    }

    // ---- findAllByTenantId (List) ----

    @Test
    void findAllByTenantId_returnsOrderedByCode() {
        createAccount(TENANT_ID, "2000", "Liabilities", AccountType.LIABILITY,
                true, false, false, true);
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        List<Account> result = accountRepository.findAllByTenantId(TENANT_ID);

        assertEquals(2, result.size());
        assertEquals("1000", result.get(0).getCode());
        assertEquals("2000", result.get(1).getCode());
    }

    // ---- findRootAccountsByTenantId ----

    @Test
    void findRootAccountsByTenantId_returnsOnlyRootAccounts() {
        Account parent = createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        Account child = Account.builder()
                .tenantId(TENANT_ID)
                .code("1001")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .parentAccount(parent)
                .accountLevel(2)
                .build();
        entityManager.persistAndFlush(child);

        List<Account> result = accountRepository.findRootAccountsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("1000", result.get(0).getCode());
    }

    // ---- findChildAccountsByParentId ----

    @Test
    void findChildAccountsByParentId_returnsChildAccounts() {
        Account parent = createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        Account child1 = Account.builder()
                .tenantId(TENANT_ID)
                .code("1001")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .parentAccount(parent)
                .accountLevel(2)
                .build();
        entityManager.persistAndFlush(child1);

        Account child2 = Account.builder()
                .tenantId(TENANT_ID)
                .code("1002")
                .name("Bank")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .parentAccount(parent)
                .accountLevel(2)
                .build();
        entityManager.persistAndFlush(child2);

        List<Account> result = accountRepository.findChildAccountsByParentId(TENANT_ID, parent.getId());

        assertEquals(2, result.size());
        assertEquals("1001", result.get(0).getCode());
        assertEquals("1002", result.get(1).getCode());
    }

    // ---- findByCodeAndTenantId ----

    @Test
    void findByCodeAndTenantId_exists_returnsAccount() {
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        Optional<Account> result = accountRepository.findByCodeAndTenantId("1000", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Assets", result.get().getName());
    }

    @Test
    void findByCodeAndTenantId_wrongTenant_returnsEmpty() {
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        Optional<Account> result = accountRepository.findByCodeAndTenantId("1000", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByAccountTypeAndTenantId ----

    @Test
    void findByAccountTypeAndTenantId_returnsMatchingType() {
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);
        createAccount(TENANT_ID, "2000", "Liabilities", AccountType.LIABILITY,
                true, false, false, true);
        createAccount(TENANT_ID, "1001", "Cash", AccountType.ASSET,
                true, false, false, true);

        List<Account> result = accountRepository.findByAccountTypeAndTenantId(TENANT_ID, AccountType.ASSET);

        assertEquals(2, result.size());
    }

    // ---- findAllActiveByTenantId ----

    @Test
    void findAllActiveByTenantId_returnsActiveOnly() {
        createAccount(TENANT_ID, "1000", "Active Account", AccountType.ASSET,
                true, false, false, true);
        createAccount(TENANT_ID, "1001", "Inactive Account", AccountType.ASSET,
                false, false, false, true);

        List<Account> result = accountRepository.findAllActiveByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Active Account", result.get(0).getName());
    }

    // ---- findAllPostableByTenantId ----

    @Test
    void findAllPostableByTenantId_returnsPostableOnly() {
        createAccount(TENANT_ID, "1000", "Postable", AccountType.ASSET,
                true, false, false, true);
        createAccount(TENANT_ID, "1001", "Non-Postable", AccountType.ASSET,
                true, false, false, false);

        List<Account> result = accountRepository.findAllPostableByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Postable", result.get(0).getName());
    }

    // ---- searchByTenantId ----

    @Test
    void searchByTenantId_byName_returnsMatching() {
        createAccount(TENANT_ID, "1000", "Cash and Equivalents", AccountType.ASSET,
                true, false, false, true);
        createAccount(TENANT_ID, "2000", "Accounts Payable", AccountType.LIABILITY,
                true, false, false, true);

        Page<Account> result = accountRepository.searchByTenantId(
                TENANT_ID, "cash", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Cash and Equivalents", result.getContent().get(0).getName());
    }

    @Test
    void searchByTenantId_byCode_returnsMatching() {
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        Page<Account> result = accountRepository.searchByTenantId(
                TENANT_ID, "1000", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    // ---- existsByCodeAndTenantId ----

    @Test
    void existsByCodeAndTenantId_exists_returnsTrue() {
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);

        assertTrue(accountRepository.existsByCodeAndTenantId("1000", TENANT_ID));
    }

    @Test
    void existsByCodeAndTenantId_notExists_returnsFalse() {
        assertFalse(accountRepository.existsByCodeAndTenantId("9999", TENANT_ID));
    }

    // ---- findControlAccountsByTenantId ----

    @Test
    void findControlAccountsByTenantId_returnsControlAccounts() {
        createAccount(TENANT_ID, "1100", "AR Control", AccountType.ASSET,
                true, true, false, true);
        createAccount(TENANT_ID, "1000", "Cash", AccountType.ASSET,
                true, false, false, true);

        List<Account> result = accountRepository.findControlAccountsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("AR Control", result.get(0).getName());
    }

    // ---- findSystemAccountsByTenantId ----

    @Test
    void findSystemAccountsByTenantId_returnsSystemAccounts() {
        createAccount(TENANT_ID, "3000", "Retained Earnings", AccountType.EQUITY,
                true, false, true, false);
        createAccount(TENANT_ID, "1000", "Cash", AccountType.ASSET,
                true, false, false, true);

        List<Account> result = accountRepository.findSystemAccountsByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Retained Earnings", result.get(0).getName());
    }

    // ---- countByTenantId ----

    @Test
    void countByTenantId_returnsCorrectCount() {
        createAccount(TENANT_ID, "1000", "Assets", AccountType.ASSET,
                true, false, false, true);
        createAccount(TENANT_ID, "2000", "Liabilities", AccountType.LIABILITY,
                true, false, false, true);
        createAccount(OTHER_TENANT_ID, "1000", "Other Assets", AccountType.ASSET,
                true, false, false, true);

        long result = accountRepository.countByTenantId(TENANT_ID);

        assertEquals(2, result);
    }
}
