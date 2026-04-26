package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.BankAccount;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import com.hisobnoma.platform.finance.entity.BankReconciliation;
import com.hisobnoma.platform.finance.entity.ReconciliationStatus;
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
class BankReconciliationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BankReconciliationRepository bankReconciliationRepository;

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

    private BankReconciliation createReconciliation(Long tenantId, BankAccount bankAccount,
                                                     String reconciliationNumber,
                                                     LocalDate statementDate,
                                                     ReconciliationStatus status) {
        BankReconciliation recon = BankReconciliation.builder()
                .tenantId(tenantId)
                .bankAccount(bankAccount)
                .reconciliationNumber(reconciliationNumber)
                .statementDate(statementDate)
                .statementStartDate(statementDate.withDayOfMonth(1))
                .statementEndDate(statementDate)
                .status(status)
                .openingBalance(BigDecimal.valueOf(10000))
                .statementEndingBalance(BigDecimal.valueOf(15000))
                .build();
        return entityManager.persistAndFlush(recon);
    }

    // ---- findByIdAndTenantId ----

    @Test
    void findByIdAndTenantId_exists_returnsReconciliation() {
        BankReconciliation recon = createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.DRAFT);

        Optional<BankReconciliation> result = bankReconciliationRepository.findByIdAndTenantId(
                recon.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("REC-001", result.get().getReconciliationNumber());
    }

    // ---- findByBankAccountIdAndTenantId (List) ----

    @Test
    void findByBankAccountIdAndTenantId_returnsForAccount() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount2, "REC-002",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.COMPLETED);

        List<BankReconciliation> result = bankReconciliationRepository.findByBankAccountIdAndTenantId(
                bankAccount1.getId(), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("REC-001", result.get(0).getReconciliationNumber());
    }

    // ---- findByBankAccountIdAndStatusAndTenantId ----

    @Test
    void findByBankAccountIdAndStatusAndTenantId_returnsMatchingStatus() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 2, 28), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-002",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.DRAFT);

        List<BankReconciliation> result = bankReconciliationRepository.findByBankAccountIdAndStatusAndTenantId(
                bankAccount1.getId(), ReconciliationStatus.DRAFT, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("REC-002", result.get(0).getReconciliationNumber());
    }

    // ---- findLastCompletedByBankAccountId ----

    @Test
    void findLastCompletedByBankAccountId_returnsLatestCompleted() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 1, 31), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-002",
                LocalDate.of(2024, 2, 28), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-003",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.DRAFT);

        Optional<BankReconciliation> result = bankReconciliationRepository.findLastCompletedByBankAccountId(
                bankAccount1.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("REC-002", result.get().getReconciliationNumber());
    }

    // ---- findInProgressByBankAccountId ----

    @Test
    void findInProgressByBankAccountId_returnsDraftOrInProgress() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 2, 28), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-002",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.DRAFT);

        Optional<BankReconciliation> result = bankReconciliationRepository.findInProgressByBankAccountId(
                bankAccount1.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("REC-002", result.get().getReconciliationNumber());
    }

    @Test
    void findInProgressByBankAccountId_noInProgress_returnsEmpty() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 2, 28), ReconciliationStatus.COMPLETED);

        Optional<BankReconciliation> result = bankReconciliationRepository.findInProgressByBankAccountId(
                bankAccount1.getId(), TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByDateRangeAndTenantId ----

    @Test
    void findByDateRangeAndTenantId_returnsInRange() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 1, 31), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-002",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-003",
                LocalDate.of(2024, 6, 30), ReconciliationStatus.COMPLETED);

        List<BankReconciliation> result = bankReconciliationRepository.findByDateRangeAndTenantId(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31), TENANT_ID);

        assertEquals(2, result.size());
    }

    // ---- findMaxReconciliationNumberByTenantId ----

    @Test
    void findMaxReconciliationNumberByTenantId_returnsMax() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 1, 31), ReconciliationStatus.COMPLETED);
        createReconciliation(TENANT_ID, bankAccount1, "REC-050",
                LocalDate.of(2024, 2, 28), ReconciliationStatus.COMPLETED);

        String result = bankReconciliationRepository.findMaxReconciliationNumberByTenantId(TENANT_ID);

        assertEquals("REC-050", result);
    }

    @Test
    void findMaxReconciliationNumberByTenantId_noReconciliations_returnsNull() {
        String result = bankReconciliationRepository.findMaxReconciliationNumberByTenantId(TENANT_ID);

        assertNull(result);
    }

    // ---- existsByReconciliationNumberAndTenantId ----

    @Test
    void existsByReconciliationNumberAndTenantId_exists_returnsTrue() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 1, 31), ReconciliationStatus.DRAFT);

        assertTrue(bankReconciliationRepository.existsByReconciliationNumberAndTenantId("REC-001", TENANT_ID));
    }

    @Test
    void existsByReconciliationNumberAndTenantId_notExists_returnsFalse() {
        assertFalse(bankReconciliationRepository.existsByReconciliationNumberAndTenantId("REC-999", TENANT_ID));
    }

    // ---- hasInProgressReconciliation ----

    @Test
    void hasInProgressReconciliation_hasDraft_returnsTrue() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.DRAFT);

        assertTrue(bankReconciliationRepository.hasInProgressReconciliation(bankAccount1.getId(), TENANT_ID));
    }

    @Test
    void hasInProgressReconciliation_hasInProgress_returnsTrue() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.IN_PROGRESS);

        assertTrue(bankReconciliationRepository.hasInProgressReconciliation(bankAccount1.getId(), TENANT_ID));
    }

    @Test
    void hasInProgressReconciliation_onlyCompleted_returnsFalse() {
        createReconciliation(TENANT_ID, bankAccount1, "REC-001",
                LocalDate.of(2024, 3, 31), ReconciliationStatus.COMPLETED);

        assertFalse(bankReconciliationRepository.hasInProgressReconciliation(bankAccount1.getId(), TENANT_ID));
    }
}
