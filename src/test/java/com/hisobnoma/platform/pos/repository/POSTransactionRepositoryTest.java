package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.pos.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class POSTransactionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private POSTransactionRepository posTransactionRepository;

    private static final Long TENANT_ID = 1L;

    private Location location;
    private POSTerminal terminal;
    private Shift shift;
    private Customer customer;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .code("LOC1").name("Store 1").locationType(LocationType.STORE).build();
        location.setTenantId(TENANT_ID);
        location = em.persistAndFlush(location);

        terminal = POSTerminal.builder()
                .terminalCode("T001").name("Terminal 1").location(location).build();
        terminal.setTenantId(TENANT_ID);
        terminal = em.persistAndFlush(terminal);

        shift = Shift.builder()
                .shiftNumber("SH001").terminal(terminal).cashierId(100L)
                .cashierName("Cashier").status(ShiftStatus.OPEN)
                .openedAt(Instant.now()).openingCash(BigDecimal.ZERO).build();
        shift.setTenantId(TENANT_ID);
        shift = em.persistAndFlush(shift);

        customer = Customer.builder()
                .code("CUST1").name("Test Customer").build();
        customer.setTenantId(TENANT_ID);
        customer = em.persistAndFlush(customer);
    }

    private POSTransaction persistTransaction(String number, TransactionType type,
                                               TransactionStatus status, BigDecimal total,
                                               Instant completedAt, boolean glPosted) {
        POSTransaction t = POSTransaction.builder()
                .transactionNumber(number)
                .terminal(terminal).shift(shift).customer(customer)
                .customerName("Test Customer").customerPhone("123456")
                .transactionType(type).status(status)
                .subtotal(total).totalAmount(total)
                .taxAmount(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO)
                .completedAt(completedAt).glPosted(glPosted)
                .build();
        t.setTenantId(TENANT_ID);
        return em.persistAndFlush(t);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.PENDING, BigDecimal.ONE, null, false);

        Page<POSTransaction> result = posTransactionRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByTransactionNumberAndTenantId_existing_returnsTransaction() {
        persistTransaction("TX-FIND", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        Optional<POSTransaction> result = posTransactionRepository.findByTransactionNumberAndTenantId("TX-FIND", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("TX-FIND", result.get().getTransactionNumber());
    }

    @Test
    void findByShiftIdAndTenantId_returnsTransactionsForShift() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        List<POSTransaction> result = posTransactionRepository.findByShiftIdAndTenantId(shift.getId(), TENANT_ID);

        assertEquals(1, result.size());
    }

    @Test
    void findByStatusAndTenantId_filtersCorrectly() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.PENDING, BigDecimal.ONE, null, false);

        List<POSTransaction> result = posTransactionRepository.findByStatusAndTenantId(TransactionStatus.COMPLETED, TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TX001", result.get(0).getTransactionNumber());
    }

    @Test
    void findByDateRangeAndTenantId_filtersCorrectly() {
        Instant now = Instant.now();
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, now, true);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.ONE, now.minus(5, ChronoUnit.DAYS), true);

        List<POSTransaction> result = posTransactionRepository.findByDateRangeAndTenantId(
                now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TX001", result.get(0).getTransactionNumber());
    }

    @Test
    void sumSalesByShiftId_returnsTotalSales() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, new BigDecimal("100.0000"), Instant.now(), true);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.COMPLETED, new BigDecimal("200.0000"), Instant.now(), true);

        BigDecimal result = posTransactionRepository.sumSalesByShiftId(shift.getId(), TENANT_ID);

        assertEquals(0, new BigDecimal("300.0000").compareTo(result));
    }

    @Test
    void sumReturnsByShiftId_returnsTotalReturns() {
        persistTransaction("TX001", TransactionType.RETURN, TransactionStatus.COMPLETED, new BigDecimal("50.0000"), Instant.now(), true);

        BigDecimal result = posTransactionRepository.sumReturnsByShiftId(shift.getId(), TENANT_ID);

        assertEquals(0, new BigDecimal("50.0000").compareTo(result));
    }

    @Test
    void countCompletedByShiftId_returnsCount() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.PENDING, BigDecimal.ONE, null, false);

        Long result = posTransactionRepository.countCompletedByShiftId(shift.getId(), TENANT_ID);

        assertEquals(1L, result);
    }

    @Test
    void countVoidedByShiftId_returnsCount() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.VOIDED, BigDecimal.TEN, null, false);

        Long result = posTransactionRepository.countVoidedByShiftId(shift.getId(), TENANT_ID);

        assertEquals(1L, result);
    }

    @Test
    void existsByTransactionNumberAndTenantId_existing_returnsTrue() {
        persistTransaction("TX-EXISTS", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        assertTrue(posTransactionRepository.existsByTransactionNumberAndTenantId("TX-EXISTS", TENANT_ID));
        assertFalse(posTransactionRepository.existsByTransactionNumberAndTenantId("TX-NOPE", TENANT_ID));
    }

    @Test
    void findMaxTransactionNumberByPrefixAndTenantId_returnsMaxNumber() {
        persistTransaction("TX-2024-001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);
        persistTransaction("TX-2024-002", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        String result = posTransactionRepository.findMaxTransactionNumberByPrefixAndTenantId("TX-2024-", TENANT_ID);

        assertEquals("TX-2024-002", result);
    }

    @Test
    void searchByTenantId_matchesByTransactionNumber() {
        persistTransaction("SALE-001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);
        persistTransaction("RET-001", TransactionType.RETURN, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        Page<POSTransaction> result = posTransactionRepository.searchByTenantId("SALE", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findCompletedWithoutGlPosting_returnsUnpostedTransactions() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), false);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        List<POSTransaction> result = posTransactionRepository.findCompletedWithoutGlPosting(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("TX001", result.get(0).getTransactionNumber());
    }

    @Test
    void countUnresolvedByShiftId_countsCorrectStatuses() {
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.PENDING, BigDecimal.TEN, null, false);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.HELD, BigDecimal.TEN, null, false);
        persistTransaction("TX003", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, Instant.now(), true);

        Long result = posTransactionRepository.countUnresolvedByShiftId(shift.getId(), TENANT_ID);

        assertEquals(2L, result);
    }

    @Test
    void sumCompletedSalesByDateRange_returnsCorrectSum() {
        Instant now = Instant.now();
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, new BigDecimal("150.0000"), now, true);
        persistTransaction("TX002", TransactionType.SALE, TransactionStatus.COMPLETED, new BigDecimal("250.0000"), now, true);

        BigDecimal result = posTransactionRepository.sumCompletedSalesByDateRange(
                TENANT_ID, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

        assertEquals(0, new BigDecimal("400.0000").compareTo(result));
    }

    @Test
    void countCompletedSalesByDateRange_returnsCount() {
        Instant now = Instant.now();
        persistTransaction("TX001", TransactionType.SALE, TransactionStatus.COMPLETED, BigDecimal.TEN, now, true);
        persistTransaction("TX002", TransactionType.RETURN, TransactionStatus.COMPLETED, BigDecimal.TEN, now, true);

        Integer result = posTransactionRepository.countCompletedSalesByDateRange(
                TENANT_ID, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

        assertEquals(1, result);
    }
}
