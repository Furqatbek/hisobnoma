package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ARPayment;
import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.entity.ARPaymentStatus;
import com.hisobnoma.platform.finance.entity.Customer;
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
class ARPaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ARPaymentRepository arPaymentRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private Customer customer1;
    private Customer customer2;

    @BeforeEach
    void setUp() {
        customer1 = Customer.builder()
                .tenantId(TENANT_ID)
                .code("CUST-001")
                .name("Customer One")
                .build();
        customer1 = entityManager.persistAndFlush(customer1);

        customer2 = Customer.builder()
                .tenantId(TENANT_ID)
                .code("CUST-002")
                .name("Customer Two")
                .build();
        customer2 = entityManager.persistAndFlush(customer2);
    }

    private ARPayment createPayment(Long tenantId, String paymentNumber, Customer customer,
                                     ARPaymentStatus status, BigDecimal paymentAmount,
                                     LocalDate paymentDate, boolean deposited) {
        ARPayment payment = ARPayment.builder()
                .tenantId(tenantId)
                .paymentNumber(paymentNumber)
                .customer(customer)
                .customerName(customer.getName())
                .status(status)
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(paymentAmount)
                .paymentDate(paymentDate)
                .deposited(deposited)
                .build();
        return entityManager.persistAndFlush(payment);
    }

    // ---- findByPaymentNumberAndTenantId ----

    @Test
    void findByPaymentNumberAndTenantId_exists_returnsPayment() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 1), false);

        Optional<ARPayment> result = arPaymentRepository.findByPaymentNumberAndTenantId("REC-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("REC-001", result.get().getPaymentNumber());
    }

    @Test
    void findByPaymentNumberAndTenantId_wrongTenant_returnsEmpty() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 1), false);

        Optional<ARPayment> result = arPaymentRepository.findByPaymentNumberAndTenantId("REC-001", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByDateRange ----

    @Test
    void findByDateRange_returnsPaymentsInRange() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 15), false);
        createPayment(TENANT_ID, "REC-002", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(3000), LocalDate.of(2024, 5, 15), false);

        List<ARPayment> result = arPaymentRepository.findByDateRange(
                TENANT_ID, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 4, 1));

        assertEquals(1, result.size());
        assertEquals("REC-001", result.get(0).getPaymentNumber());
    }

    // ---- sumPaymentsByCustomerAndStatus ----

    @Test
    void sumPaymentsByCustomerAndStatus_returnsCorrectSum() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 1), false);
        createPayment(TENANT_ID, "REC-002", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(3000), LocalDate.of(2024, 3, 15), false);
        createPayment(TENANT_ID, "REC-003", customer1, ARPaymentStatus.CANCELLED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 20), false);

        BigDecimal result = arPaymentRepository.sumPaymentsByCustomerAndStatus(
                TENANT_ID, customer1.getId(), ARPaymentStatus.COMPLETED);

        assertEquals(0, BigDecimal.valueOf(8000).compareTo(result));
    }

    @Test
    void sumPaymentsByCustomerAndStatus_noPayments_returnsZero() {
        BigDecimal result = arPaymentRepository.sumPaymentsByCustomerAndStatus(
                TENANT_ID, customer1.getId(), ARPaymentStatus.COMPLETED);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // ---- sumPaymentsByDateRangeAndStatus ----

    @Test
    void sumPaymentsByDateRangeAndStatus_returnsCorrectSum() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 15), false);
        createPayment(TENANT_ID, "REC-002", customer2, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(3000), LocalDate.of(2024, 3, 20), false);
        createPayment(TENANT_ID, "REC-003", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 5, 1), false);

        BigDecimal result = arPaymentRepository.sumPaymentsByDateRangeAndStatus(
                TENANT_ID, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31),
                ARPaymentStatus.COMPLETED);

        assertEquals(0, BigDecimal.valueOf(8000).compareTo(result));
    }

    // ---- findByTenantIdAndDepositedFalseAndStatus ----

    @Test
    void findByTenantIdAndDepositedFalseAndStatus_returnsUndeposited() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 1), false);
        createPayment(TENANT_ID, "REC-002", customer1, ARPaymentStatus.COMPLETED,
                BigDecimal.valueOf(3000), LocalDate.of(2024, 3, 15), true);

        List<ARPayment> result = arPaymentRepository.findByTenantIdAndDepositedFalseAndStatus(
                TENANT_ID, ARPaymentStatus.COMPLETED);

        assertEquals(1, result.size());
        assertEquals("REC-001", result.get(0).getPaymentNumber());
    }

    // ---- existsByPaymentNumberAndTenantId ----

    @Test
    void existsByPaymentNumberAndTenantId_exists_returnsTrue() {
        createPayment(TENANT_ID, "REC-001", customer1, ARPaymentStatus.PENDING,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 1), false);

        assertTrue(arPaymentRepository.existsByPaymentNumberAndTenantId("REC-001", TENANT_ID));
    }

    // ---- findMaxPaymentNumber ----

    @Test
    void findMaxPaymentNumber_returnsMax() {
        createPayment(TENANT_ID, "REC-010", customer1, ARPaymentStatus.PENDING,
                BigDecimal.valueOf(5000), LocalDate.of(2024, 3, 1), false);
        createPayment(TENANT_ID, "REC-035", customer1, ARPaymentStatus.PENDING,
                BigDecimal.valueOf(3000), LocalDate.of(2024, 3, 15), false);

        Integer result = arPaymentRepository.findMaxPaymentNumber(TENANT_ID);

        assertEquals(35, result);
    }

    @Test
    void findMaxPaymentNumber_noPayments_returnsNull() {
        Integer result = arPaymentRepository.findMaxPaymentNumber(TENANT_ID);

        assertNull(result);
    }
}
