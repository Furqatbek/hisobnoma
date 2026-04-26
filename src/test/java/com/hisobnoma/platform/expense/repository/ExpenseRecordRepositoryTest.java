package com.hisobnoma.platform.expense.repository;

import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class ExpenseRecordRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ExpenseRecordRepository expenseRecordRepository;

    private static final Long TENANT_ID = 1L;

    private ExpenseRecord persistExpense(String category, BigDecimal total, Long tenantId) {
        ExpenseRecord e = ExpenseRecord.builder()
                .createDate(LocalDate.now())
                .category(category)
                .totalAmount(total)
                .currency("UZS")
                .build();
        e.setTenantId(tenantId);
        return em.persistAndFlush(e);
    }

    @Test
    void findAllByTenantId_returnsPagedResults() {
        persistExpense("TRAVEL", new BigDecimal("500000.0000"), TENANT_ID);
        persistExpense("OFFICE", new BigDecimal("300000.0000"), TENANT_ID);
        persistExpense("TRAVEL", new BigDecimal("100000.0000"), 2L);

        Page<ExpenseRecord> result = expenseRecordRepository.findAllByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByTenantIdOrTenantIdIsNull_includesNullTenant() {
        persistExpense("TRAVEL", new BigDecimal("500000.0000"), TENANT_ID);

        ExpenseRecord noTenant = ExpenseRecord.builder()
                .createDate(LocalDate.now())
                .category("SYSTEM")
                .totalAmount(new BigDecimal("100000.0000"))
                .currency("UZS").build();
        noTenant.setTenantId(null);
        em.persistAndFlush(noTenant);

        persistExpense("OFFICE", new BigDecimal("200000.0000"), 2L);

        Page<ExpenseRecord> result = expenseRecordRepository.findByTenantIdOrTenantIdIsNull(
                TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndCategory_filtersCorrectly() {
        persistExpense("TRAVEL", new BigDecimal("500000.0000"), TENANT_ID);
        persistExpense("OFFICE", new BigDecimal("300000.0000"), TENANT_ID);
        persistExpense("TRAVEL", new BigDecimal("100000.0000"), TENANT_ID);

        Page<ExpenseRecord> result = expenseRecordRepository.findByTenantIdAndCategory(
                TENANT_ID, "TRAVEL", PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void sumTotalByTenantId_returnsTotalAmount() {
        persistExpense("TRAVEL", new BigDecimal("500000.0000"), TENANT_ID);
        persistExpense("OFFICE", new BigDecimal("300000.0000"), TENANT_ID);
        persistExpense("TRAVEL", new BigDecimal("100000.0000"), 2L);

        BigDecimal result = expenseRecordRepository.sumTotalByTenantId(TENANT_ID);

        assertEquals(0, new BigDecimal("800000.0000").compareTo(result));
    }

    @Test
    void sumTotalByTenantId_noRecords_returnsZero() {
        BigDecimal result = expenseRecordRepository.sumTotalByTenantId(TENANT_ID);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void sumTotalByTenantIdOrNull_includesNullTenantRecords() {
        persistExpense("TRAVEL", new BigDecimal("500000.0000"), TENANT_ID);

        ExpenseRecord noTenant = ExpenseRecord.builder()
                .createDate(LocalDate.now())
                .category("SYSTEM")
                .totalAmount(new BigDecimal("200000.0000"))
                .currency("UZS").build();
        noTenant.setTenantId(null);
        em.persistAndFlush(noTenant);

        BigDecimal result = expenseRecordRepository.sumTotalByTenantIdOrNull(TENANT_ID);

        assertEquals(0, new BigDecimal("700000.0000").compareTo(result));
    }
}
