package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.APPayment;
import com.hisobnoma.platform.finance.entity.APPaymentMethod;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
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
class APPaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private APPaymentRepository apPaymentRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private APPayment createPayment(Long tenantId, String paymentNumber, Long vendorId,
                                     APPaymentStatus status, BigDecimal paymentAmount,
                                     LocalDate paymentDate, boolean reconciled) {
        APPayment payment = APPayment.builder()
                .tenantId(tenantId)
                .paymentNumber(paymentNumber)
                .vendorId(vendorId)
                .vendorName("Vendor " + vendorId)
                .status(status)
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(paymentAmount)
                .paymentDate(paymentDate)
                .reconciled(reconciled)
                .build();
        return entityManager.persistAndFlush(payment);
    }

    // ---- findByPaymentNumberAndTenantId ----

    @Test
    void findByPaymentNumberAndTenantId_exists_returnsPayment() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 1), false);

        Optional<APPayment> result = apPaymentRepository.findByPaymentNumberAndTenantId("PAY-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("PAY-001", result.get().getPaymentNumber());
    }

    @Test
    void findByPaymentNumberAndTenantId_wrongTenant_returnsEmpty() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 1), false);

        Optional<APPayment> result = apPaymentRepository.findByPaymentNumberAndTenantId("PAY-001", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findByDateRange ----

    @Test
    void findByDateRange_returnsPaymentsInRange() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 15), false);
        createPayment(TENANT_ID, "PAY-002", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(2000), LocalDate.of(2024, 5, 15), false);
        createPayment(TENANT_ID, "PAY-003", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(3000), LocalDate.of(2024, 1, 15), false);

        List<APPayment> result = apPaymentRepository.findByDateRange(
                TENANT_ID, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 4, 1));

        assertEquals(1, result.size());
        assertEquals("PAY-001", result.get(0).getPaymentNumber());
    }

    // ---- sumPaymentsByVendorAndStatus ----

    @Test
    void sumPaymentsByVendorAndStatus_returnsCorrectSum() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 1), false);
        createPayment(TENANT_ID, "PAY-002", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(2000), LocalDate.of(2024, 3, 15), false);
        createPayment(TENANT_ID, "PAY-003", 10L, APPaymentStatus.VOIDED,
                BigDecimal.valueOf(500), LocalDate.of(2024, 3, 20), false);

        BigDecimal result = apPaymentRepository.sumPaymentsByVendorAndStatus(
                TENANT_ID, 10L, APPaymentStatus.COMPLETED);

        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result));
    }

    @Test
    void sumPaymentsByVendorAndStatus_noPayments_returnsZero() {
        BigDecimal result = apPaymentRepository.sumPaymentsByVendorAndStatus(
                TENANT_ID, 10L, APPaymentStatus.COMPLETED);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // ---- sumPaymentsByDateRangeAndStatus ----

    @Test
    void sumPaymentsByDateRangeAndStatus_returnsCorrectSum() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 15), false);
        createPayment(TENANT_ID, "PAY-002", 20L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(2000), LocalDate.of(2024, 3, 20), false);
        createPayment(TENANT_ID, "PAY-003", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(500), LocalDate.of(2024, 5, 1), false);

        BigDecimal result = apPaymentRepository.sumPaymentsByDateRangeAndStatus(
                TENANT_ID, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31),
                APPaymentStatus.COMPLETED);

        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result));
    }

    // ---- findByTenantIdAndReconciledFalseAndStatus ----

    @Test
    void findByTenantIdAndReconciledFalseAndStatus_returnsUnreconciled() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 1), false);
        createPayment(TENANT_ID, "PAY-002", 10L, APPaymentStatus.COMPLETED,
                BigDecimal.valueOf(2000), LocalDate.of(2024, 3, 15), true);

        List<APPayment> result = apPaymentRepository.findByTenantIdAndReconciledFalseAndStatus(
                TENANT_ID, APPaymentStatus.COMPLETED);

        assertEquals(1, result.size());
        assertEquals("PAY-001", result.get(0).getPaymentNumber());
    }

    // ---- existsByPaymentNumberAndTenantId ----

    @Test
    void existsByPaymentNumberAndTenantId_exists_returnsTrue() {
        createPayment(TENANT_ID, "PAY-001", 10L, APPaymentStatus.DRAFT,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 1), false);

        assertTrue(apPaymentRepository.existsByPaymentNumberAndTenantId("PAY-001", TENANT_ID));
    }

    @Test
    void existsByPaymentNumberAndTenantId_notExists_returnsFalse() {
        assertFalse(apPaymentRepository.existsByPaymentNumberAndTenantId("PAY-999", TENANT_ID));
    }

    // ---- findMaxPaymentNumber ----

    @Test
    void findMaxPaymentNumber_returnsMax() {
        createPayment(TENANT_ID, "PAY-005", 10L, APPaymentStatus.DRAFT,
                BigDecimal.valueOf(1000), LocalDate.of(2024, 3, 1), false);
        createPayment(TENANT_ID, "PAY-023", 10L, APPaymentStatus.DRAFT,
                BigDecimal.valueOf(2000), LocalDate.of(2024, 3, 15), false);

        Integer result = apPaymentRepository.findMaxPaymentNumber(TENANT_ID);

        assertEquals(23, result);
    }

    @Test
    void findMaxPaymentNumber_noPayments_returnsNull() {
        Integer result = apPaymentRepository.findMaxPaymentNumber(TENANT_ID);

        assertNull(result);
    }
}
