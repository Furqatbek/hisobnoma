package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
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
class ARInvoiceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ARInvoiceRepository arInvoiceRepository;

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

    private ARInvoice createInvoice(Long tenantId, String invoiceNumber, Customer customer,
                                     ARInvoiceStatus status, BigDecimal totalAmount,
                                     BigDecimal balanceDue, LocalDate invoiceDate,
                                     LocalDate dueDate) {
        ARInvoice invoice = ARInvoice.builder()
                .tenantId(tenantId)
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .customerName(customer.getName())
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
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        Optional<ARInvoice> result = arInvoiceRepository.findByInvoiceNumberAndTenantId("INV-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("INV-001", result.get().getInvoiceNumber());
    }

    // ---- findUnpaidInvoices ----

    @Test
    void findUnpaidInvoices_returnsInvoicesWithBalanceDue() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-002", customer1, ARInvoiceStatus.PAID,
                BigDecimal.valueOf(3000), BigDecimal.ZERO,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-003", customer2, ARInvoiceStatus.PARTIAL,
                BigDecimal.valueOf(4000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1));

        List<ARInvoice> result = arInvoiceRepository.findUnpaidInvoices(
                TENANT_ID, List.of(ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL));

        assertEquals(2, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
        assertEquals("INV-003", result.get(1).getInvoiceNumber());
    }

    // ---- findUnpaidInvoicesAsOf ----

    @Test
    void findUnpaidInvoicesAsOf_filtersCorrectly() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-002", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1));

        List<ARInvoice> result = arInvoiceRepository.findUnpaidInvoicesAsOf(
                TENANT_ID, LocalDate.of(2024, 3, 1),
                List.of(ARInvoiceStatus.CANCELLED, ARInvoiceStatus.WRITTEN_OFF));

        assertEquals(1, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
    }

    // ---- findUnpaidByCustomer ----

    @Test
    void findUnpaidByCustomer_filtersCorrectly() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-002", customer2, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        List<ARInvoice> result = arInvoiceRepository.findUnpaidByCustomer(
                TENANT_ID, customer1.getId(), List.of(ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL));

        assertEquals(1, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
    }

    // ---- findOverdueInvoices ----

    @Test
    void findOverdueInvoices_returnsOverdueOnly() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15));
        createInvoice(TENANT_ID, "INV-002", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

        List<ARInvoice> result = arInvoiceRepository.findOverdueInvoices(
                TENANT_ID, LocalDate.of(2024, 6, 1), List.of(ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL));

        assertEquals(1, result.size());
        assertEquals("INV-001", result.get(0).getInvoiceNumber());
    }

    // ---- sumBalanceDueByCustomer ----

    @Test
    void sumBalanceDueByCustomer_returnsCorrectSum() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-002", customer1, ARInvoiceStatus.PARTIAL,
                BigDecimal.valueOf(4000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1));

        BigDecimal result = arInvoiceRepository.sumBalanceDueByCustomer(
                TENANT_ID, customer1.getId(), List.of(ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL));

        assertEquals(0, BigDecimal.valueOf(5000).compareTo(result));
    }

    // ---- findLatestInvoiceDateByCustomer ----

    @Test
    void findLatestInvoiceDateByCustomer_returnsLatestDate() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-002", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 6, 15), LocalDate.of(2024, 7, 15));

        LocalDate result = arInvoiceRepository.findLatestInvoiceDateByCustomer(
                TENANT_ID, customer1.getId());

        assertEquals(LocalDate.of(2024, 6, 15), result);
    }

    // ---- sumTotalBalanceDue ----

    @Test
    void sumTotalBalanceDue_returnsCorrectSum() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-002", customer2, ARInvoiceStatus.PARTIAL,
                BigDecimal.valueOf(4000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1));

        BigDecimal result = arInvoiceRepository.sumTotalBalanceDue(
                TENANT_ID, List.of(ARInvoiceStatus.CANCELLED, ARInvoiceStatus.WRITTEN_OFF));

        assertEquals(0, BigDecimal.valueOf(5000).compareTo(result));
    }

    // ---- sumBalanceDueByDateRange ----

    @Test
    void sumBalanceDueByDateRange_returnsCorrectSum() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 15));
        createInvoice(TENANT_ID, "INV-002", customer2, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(4000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 15));

        BigDecimal result = arInvoiceRepository.sumBalanceDueByDateRange(
                TENANT_ID, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1),
                List.of(ARInvoiceStatus.CANCELLED, ARInvoiceStatus.WRITTEN_OFF));

        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result));
    }

    // ---- sumOverdueBalance ----

    @Test
    void sumOverdueBalance_returnsCorrectSum() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15));
        createInvoice(TENANT_ID, "INV-002", customer2, ARInvoiceStatus.SENT,
                BigDecimal.valueOf(4000), BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

        BigDecimal result = arInvoiceRepository.sumOverdueBalance(
                TENANT_ID, LocalDate.of(2024, 6, 1),
                List.of(ARInvoiceStatus.CANCELLED, ARInvoiceStatus.WRITTEN_OFF));

        assertEquals(0, BigDecimal.valueOf(3000).compareTo(result));
    }

    // ---- existsByInvoiceNumberAndTenantId ----

    @Test
    void existsByInvoiceNumberAndTenantId_exists_returnsTrue() {
        createInvoice(TENANT_ID, "INV-001", customer1, ARInvoiceStatus.DRAFT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        assertTrue(arInvoiceRepository.existsByInvoiceNumberAndTenantId("INV-001", TENANT_ID));
    }

    // ---- findMaxInvoiceNumber ----

    @Test
    void findMaxInvoiceNumber_returnsMaxNumber() {
        createInvoice(TENANT_ID, "INV-010", customer1, ARInvoiceStatus.DRAFT,
                BigDecimal.valueOf(5000), BigDecimal.valueOf(5000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
        createInvoice(TENANT_ID, "INV-055", customer1, ARInvoiceStatus.DRAFT,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(3000),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));

        Integer result = arInvoiceRepository.findMaxInvoiceNumber(TENANT_ID);

        assertEquals(55, result);
    }

    @Test
    void findMaxInvoiceNumber_noInvoices_returnsNull() {
        Integer result = arInvoiceRepository.findMaxInvoiceNumber(TENANT_ID);

        assertNull(result);
    }
}
