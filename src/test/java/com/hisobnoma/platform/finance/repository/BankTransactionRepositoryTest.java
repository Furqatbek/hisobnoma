package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class BankTransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private BankAccount bankAccount1;
    private BankAccount bankAccount2;

    @BeforeEach
    void setUp() {
        bankAccount1 = BankAccount.builder()
                .tenantId(TENANT_ID)
                .accountCode("BA-001")
                .accountName("Main Checking")
                .accountType(BankAccountType.CHECKING)
                .build();
        bankAccount1 = entityManager.persistAndFlush(bankAccount1);

        bankAccount2 = BankAccount.builder()
                .tenantId(TENANT_ID)
                .accountCode("BA-002")
                .accountName("Savings")
                .accountType(BankAccountType.SAVINGS)
                .build();
        bankAccount2 = entityManager.persistAndFlush(bankAccount2);
    }

    private BankTransaction createTransaction(Long tenantId, BankAccount bankAccount,
                                               String transactionNumber, LocalDate transactionDate,
                                               BankTransactionType type, BankTransactionStatus status,
                                               BigDecimal debitAmount, BigDecimal creditAmount) {
        BankTransaction tx = BankTransaction.builder()
                .tenantId(tenantId)
                .bankAccount(bankAccount)
                .transactionNumber(transactionNumber)
                .transactionDate(transactionDate)
                .transactionType(type)
                .status(status)
                .description("Transaction " + transactionNumber)
                .debitAmount(debitAmount)
                .creditAmount(creditAmount)
                .build();
        return entityManager.persistAndFlush(tx);
    }

    // ---- findByIdAndTenantId ----

    @Test
    void findByIdAndTenantId_exists_returnsTransaction() {
        BankTransaction tx = createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));

        Optional<BankTransaction> result = bankTransactionRepository.findByIdAndTenantId(tx.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("TXN-001", result.get().getTransactionNumber());
    }

    // ---- findByBankAccountIdAndTenantId (List) ----

    @Test
    void findByBankAccountIdAndTenantId_returnsTransactionsForAccount() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));
        createTransaction(TENANT_ID, bankAccount2, "TXN-002",
                LocalDate.of(2024, 3, 16), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(3000));

        List<BankTransaction> result = bankTransactionRepository.findByBankAccountIdAndTenantId(
                bankAccount1.getId(), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TXN-001", result.get(0).getTransactionNumber());
    }

    // ---- findByBankAccountIdAndDateRangeAndTenantId ----

    @Test
    void findByBankAccountIdAndDateRangeAndTenantId_returnsInRange() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-002",
                LocalDate.of(2024, 5, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(3000));

        List<BankTransaction> result = bankTransactionRepository.findByBankAccountIdAndDateRangeAndTenantId(
                bankAccount1.getId(), LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TXN-001", result.get(0).getTransactionNumber());
    }

    // ---- findByBankAccountIdAndStatusAndTenantId ----

    @Test
    void findByBankAccountIdAndStatusAndTenantId_returnsMatchingStatus() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-002",
                LocalDate.of(2024, 3, 16), BankTransactionType.DEPOSIT,
                BankTransactionStatus.RECONCILED, BigDecimal.ZERO, BigDecimal.valueOf(3000));

        List<BankTransaction> result = bankTransactionRepository.findByBankAccountIdAndStatusAndTenantId(
                bankAccount1.getId(), BankTransactionStatus.PENDING, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TXN-001", result.get(0).getTransactionNumber());
    }

    // ---- findUnreconciledByBankAccountIdAndTenantId ----

    @Test
    void findUnreconciledByBankAccountIdAndTenantId_returnsUnreconciled() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-002",
                LocalDate.of(2024, 3, 16), BankTransactionType.DEPOSIT,
                BankTransactionStatus.CLEARED, BigDecimal.ZERO, BigDecimal.valueOf(3000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-003",
                LocalDate.of(2024, 3, 17), BankTransactionType.DEPOSIT,
                BankTransactionStatus.RECONCILED, BigDecimal.ZERO, BigDecimal.valueOf(2000));

        List<BankTransaction> result = bankTransactionRepository.findUnreconciledByBankAccountIdAndTenantId(
                bankAccount1.getId(),
                List.of(BankTransactionStatus.PENDING, BankTransactionStatus.CLEARED),
                LocalDate.of(2024, 3, 31), TENANT_ID);

        assertEquals(2, result.size());
    }

    // ---- findByTransactionTypeAndTenantId ----

    @Test
    void findByTransactionTypeAndTenantId_returnsMatchingType() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-002",
                LocalDate.of(2024, 3, 16), BankTransactionType.WITHDRAWAL,
                BankTransactionStatus.PENDING, BigDecimal.valueOf(1000), BigDecimal.ZERO);

        List<BankTransaction> result = bankTransactionRepository.findByTransactionTypeAndTenantId(
                BankTransactionType.DEPOSIT, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TXN-001", result.get(0).getTransactionNumber());
    }

    // ---- findByReferenceAndTenantId ----

    @Test
    void findByReferenceAndTenantId_returnsMatchingReference() {
        BankTransaction tx = BankTransaction.builder()
                .tenantId(TENANT_ID)
                .bankAccount(bankAccount1)
                .transactionNumber("TXN-001")
                .transactionDate(LocalDate.of(2024, 3, 15))
                .transactionType(BankTransactionType.PAYMENT)
                .status(BankTransactionStatus.PENDING)
                .description("AP Payment")
                .referenceType("AP_PAYMENT")
                .referenceId(100L)
                .build();
        entityManager.persistAndFlush(tx);

        List<BankTransaction> result = bankTransactionRepository.findByReferenceAndTenantId(
                "AP_PAYMENT", 100L, TENANT_ID);

        assertEquals(1, result.size());
    }

    // ---- calculateNetBalanceByBankAccountId ----

    @Test
    void calculateNetBalanceByBankAccountId_returnsNetBalance() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(10000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-002",
                LocalDate.of(2024, 3, 16), BankTransactionType.WITHDRAWAL,
                BankTransactionStatus.PENDING, BigDecimal.valueOf(3000), BigDecimal.ZERO);
        createTransaction(TENANT_ID, bankAccount1, "TXN-003",
                LocalDate.of(2024, 3, 17), BankTransactionType.WITHDRAWAL,
                BankTransactionStatus.VOIDED, BigDecimal.valueOf(1000), BigDecimal.ZERO);

        BigDecimal result = bankTransactionRepository.calculateNetBalanceByBankAccountId(
                bankAccount1.getId(), TENANT_ID);

        // 10000 - 3000 = 7000 (voided excluded)
        assertEquals(0, BigDecimal.valueOf(7000).compareTo(result));
    }

    // ---- findMaxTransactionNumberByTenantId ----

    @Test
    void findMaxTransactionNumberByTenantId_returnsMax() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));
        createTransaction(TENANT_ID, bankAccount1, "TXN-099",
                LocalDate.of(2024, 3, 16), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(3000));

        String result = bankTransactionRepository.findMaxTransactionNumberByTenantId(TENANT_ID);

        assertEquals("TXN-099", result);
    }

    @Test
    void findMaxTransactionNumberByTenantId_noTransactions_returnsNull() {
        String result = bankTransactionRepository.findMaxTransactionNumberByTenantId(TENANT_ID);

        assertNull(result);
    }

    // ---- existsByTransactionNumberAndTenantId ----

    @Test
    void existsByTransactionNumberAndTenantId_exists_returnsTrue() {
        createTransaction(TENANT_ID, bankAccount1, "TXN-001",
                LocalDate.of(2024, 3, 15), BankTransactionType.DEPOSIT,
                BankTransactionStatus.PENDING, BigDecimal.ZERO, BigDecimal.valueOf(5000));

        assertTrue(bankTransactionRepository.existsByTransactionNumberAndTenantId("TXN-001", TENANT_ID));
    }

    @Test
    void existsByTransactionNumberAndTenantId_notExists_returnsFalse() {
        assertFalse(bankTransactionRepository.existsByTransactionNumberAndTenantId("TXN-999", TENANT_ID));
    }
}
