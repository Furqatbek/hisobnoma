package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
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
class APInvoiceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private APInvoiceRepository apInvoiceRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private APInvoice createInvoice(Long tenantId, String invoiceNumber, Long vendorId,
                                     APInvoiceStatus status, BigDecimal totalAmount,
                                     BigDecimal balanceDue, LocalDate invoiceDate,
                                     LocalDate dueDate) {
        APInvoice invoice = APInvoice.builder()
                .tenantId(tenantId)
                .invoiceNumber(invoiceNumber)
                .vendorId(vendorId)
                .vendorName("Vendor " + vendorId)
                .status(status)
                .totalAmount(totalAmount)
                .balanceDue(balanceDue)
                .subtotal(totalAmount)
                .invoiceDate(invoiceDate)
                .dueDate(dueDate)
                .build();
        return entityManager.persistAndFlush(invoice);
    }

    // ---- findByInvoiceNumberAndTenantId ----

    @Test
    void findByInvoiceNumberAndTenantId_exists_returnsInvoice() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.DRAFT,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        Optional<APInvoice> result = apInvoiceRepository.findByInvoiceNumberAndTenantId("AP-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("AP-001", result.get().getInvoiceNumber());
    }

    @Test
    void findByInvoiceNumberAndTenantId_wrongTenant_returnsEmpty() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.DRAFT,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        Optional<APInvoice> result = apInvoiceRepository.findByInvoiceNumberAndTenantId("AP-001", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findUnpaidInvoices ----

    @Test
    void findUnpaidInvoices_returnsInvoicesWithBalanceDue() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-002", 10L, APInvoiceStatus.PAID,
                BigDecimal.valueOf(2000), BigDecimal.ZERO,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-003", 10L, APInvoiceStatus.PARTIAL,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(1500),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1));

        List<APInvoice> result = apInvoiceRepository.findUnpaidInvoices(
                TENANT_ID, List.of(APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL));

        assertEquals(2, result.size());
        // Ordered by dueDate ASC
        assertEquals("AP-001", result.get(0).getInvoiceNumber());
        assertEquals("AP-003", result.get(1).getInvoiceNumber());
    }

    // ---- findUnpaidInvoicesAsOf ----

    @Test
    void findUnpaidInvoicesAsOf_filtersCorrectly() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-002", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1));

        List<APInvoice> result = apInvoiceRepository.findUnpaidInvoicesAsOf(
                TENANT_ID, LocalDate.of(2024, 3, 1), List.of(APInvoiceStatus.CANCELLED));

        assertEquals(1, result.size());
        assertEquals("AP-001", result.get(0).getInvoiceNumber());
    }

    // ---- findUnpaidInvoicesByVendor ----

    @Test
    void findUnpaidInvoicesByVendor_filtersCorrectly() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-002", 20L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        List<APInvoice> result = apInvoiceRepository.findUnpaidInvoicesByVendor(
                TENANT_ID, 10L, List.of(APInvoiceStatus.APPROVED));

        assertEquals(1, result.size());
        assertEquals("AP-001", result.get(0).getInvoiceNumber());
    }

    // ---- findOverdueInvoices ----

    @Test
    void findOverdueInvoices_returnsOverdueOnly() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15));
        createInvoice(TENANT_ID, "AP-002", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

        List<APInvoice> result = apInvoiceRepository.findOverdueInvoices(
                TENANT_ID, LocalDate.of(2024, 6, 1), List.of(APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID));

        assertEquals(1, result.size());
        assertEquals("AP-001", result.get(0).getInvoiceNumber());
    }

    // ---- sumBalanceDueByVendor ----

    @Test
    void sumBalanceDueByVendor_returnsCorrectSum() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-002", 10L, APInvoiceStatus.PARTIAL,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(800),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        BigDecimal result = apInvoiceRepository.sumBalanceDueByVendor(
                TENANT_ID, 10L, List.of(APInvoiceStatus.CANCELLED));

        assertEquals(0, BigDecimal.valueOf(1300).compareTo(result));
    }

    // ---- sumTotalBalanceDue ----

    @Test
    void sumTotalBalanceDue_returnsCorrectSum() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-002", 20L, APInvoiceStatus.PARTIAL,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(800),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        BigDecimal result = apInvoiceRepository.sumTotalBalanceDue(
                TENANT_ID, List.of(APInvoiceStatus.CANCELLED));

        assertEquals(0, BigDecimal.valueOf(1300).compareTo(result));
    }

    @Test
    void sumTotalBalanceDue_noInvoices_returnsZero() {
        BigDecimal result = apInvoiceRepository.sumTotalBalanceDue(
                TENANT_ID, List.of(APInvoiceStatus.CANCELLED));

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // ---- sumBalanceDueByDateRange ----

    @Test
    void sumBalanceDueByDateRange_returnsCorrectSum() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 15));
        createInvoice(TENANT_ID, "AP-002", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(800),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 15));

        BigDecimal result = apInvoiceRepository.sumBalanceDueByDateRange(
                TENANT_ID, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1),
                List.of(APInvoiceStatus.CANCELLED));

        assertEquals(0, BigDecimal.valueOf(500).compareTo(result));
    }

    // ---- sumOverdueBalance ----

    @Test
    void sumOverdueBalance_returnsCorrectSum() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15));
        createInvoice(TENANT_ID, "AP-002", 10L, APInvoiceStatus.APPROVED,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(800),
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

        BigDecimal result = apInvoiceRepository.sumOverdueBalance(
                TENANT_ID, LocalDate.of(2024, 6, 1), List.of(APInvoiceStatus.CANCELLED));

        assertEquals(0, BigDecimal.valueOf(500).compareTo(result));
    }

    // ---- existsByInvoiceNumberAndTenantId ----

    @Test
    void existsByInvoiceNumberAndTenantId_exists_returnsTrue() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.DRAFT,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        assertTrue(apInvoiceRepository.existsByInvoiceNumberAndTenantId("AP-001", TENANT_ID));
    }

    @Test
    void existsByInvoiceNumberAndTenantId_notExists_returnsFalse() {
        assertFalse(apInvoiceRepository.existsByInvoiceNumberAndTenantId("AP-999", TENANT_ID));
    }

    // ---- findMaxInvoiceNumber ----

    @Test
    void findMaxInvoiceNumber_returnsMaxNumber() {
        createInvoice(TENANT_ID, "AP-001", 10L, APInvoiceStatus.DRAFT,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "AP-042", 10L, APInvoiceStatus.DRAFT,
                BigDecimal.valueOf(2000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        Integer result = apInvoiceRepository.findMaxInvoiceNumber(TENANT_ID);

        assertEquals(42, result);
    }

    @Test
    void findMaxInvoiceNumber_noInvoices_returnsNull() {
        Integer result = apInvoiceRepository.findMaxInvoiceNumber(TENANT_ID);

        assertNull(result);
    }
}
