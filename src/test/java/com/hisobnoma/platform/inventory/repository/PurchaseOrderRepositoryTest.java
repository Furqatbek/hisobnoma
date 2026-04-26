package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.*;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class PurchaseOrderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    private Vendor vendor;
    private Location location;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .code("PO-V1").name("PO Vendor").active(true).build();
        vendor.setTenantId(1L);
        vendor = em.persistAndFlush(vendor);

        location = Location.builder()
                .code("PO-WH1").name("PO Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);
    }

    private PurchaseOrder persistPO(String poNumber, POStatus status, LocalDate orderDate,
                                    LocalDate expectedDate, BigDecimal totalAmount) {
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .vendor(vendor)
                .location(location)
                .status(status)
                .orderDate(orderDate)
                .expectedDate(expectedDate)
                .totalAmount(totalAmount)
                .build();
        po.setTenantId(1L);
        return em.persistAndFlush(po);
    }

    @Test
    void findByIdAndTenantId_existingPO_returnsPO() {
        PurchaseOrder po = persistPO("PO-001", POStatus.DRAFT,
                LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);

        Optional<PurchaseOrder> result = purchaseOrderRepository.findByIdAndTenantId(po.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        PurchaseOrder po = persistPO("PO-002", POStatus.DRAFT,
                LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);

        Optional<PurchaseOrder> result = purchaseOrderRepository.findByIdAndTenantId(po.getId(), 99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByTenantId_returnsAllForTenant() {
        persistPO("PO-A1", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);
        persistPO("PO-A2", POStatus.APPROVED, LocalDate.of(2024, 1, 2), null, BigDecimal.ZERO);

        Page<PurchaseOrder> result = purchaseOrderRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByPoNumberAndTenantId_existingNumber_returnsPO() {
        persistPO("PO-FIND", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);

        Optional<PurchaseOrder> result = purchaseOrderRepository
                .findByPoNumberAndTenantId("PO-FIND", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByStatusAndTenantId_filtersByStatus() {
        persistPO("PO-D1", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);
        persistPO("PO-AP1", POStatus.APPROVED, LocalDate.of(2024, 1, 2), null, BigDecimal.ZERO);

        Page<PurchaseOrder> result = purchaseOrderRepository.findByStatusAndTenantId(
                POStatus.DRAFT, 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByVendorIdAndTenantId_filtersByVendor() {
        persistPO("PO-V1A", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);

        Vendor vendor2 = Vendor.builder().code("PO-V2").name("Vendor 2").active(true).build();
        vendor2.setTenantId(1L);
        vendor2 = em.persistAndFlush(vendor2);

        PurchaseOrder po2 = PurchaseOrder.builder()
                .poNumber("PO-V2A").vendor(vendor2).location(location)
                .status(POStatus.DRAFT).orderDate(LocalDate.of(2024, 1, 1))
                .build();
        po2.setTenantId(1L);
        em.persistAndFlush(po2);

        Page<PurchaseOrder> result = purchaseOrderRepository.findByVendorIdAndTenantId(
                vendor.getId(), 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByStatusInAndTenantId_matchesMultipleStatuses() {
        persistPO("PO-M1", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);
        persistPO("PO-M2", POStatus.APPROVED, LocalDate.of(2024, 1, 2), null, BigDecimal.ZERO);
        persistPO("PO-M3", POStatus.CANCELLED, LocalDate.of(2024, 1, 3), null, BigDecimal.ZERO);

        Page<PurchaseOrder> result = purchaseOrderRepository.findByStatusInAndTenantId(
                List.of(POStatus.DRAFT, POStatus.APPROVED), 1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByDateRangeAndTenantId_filtersCorrectly() {
        persistPO("PO-DR1", POStatus.DRAFT, LocalDate.of(2024, 1, 15), null, BigDecimal.ZERO);
        persistPO("PO-DR2", POStatus.DRAFT, LocalDate.of(2024, 3, 15), null, BigDecimal.ZERO);
        persistPO("PO-DR3", POStatus.DRAFT, LocalDate.of(2024, 5, 15), null, BigDecimal.ZERO);

        Page<PurchaseOrder> result = purchaseOrderRepository.findByDateRangeAndTenantId(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31), 1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findOverdueByTenantId_returnsApprovedOrPartialPastExpectedDate() {
        persistPO("PO-OD1", POStatus.APPROVED,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), BigDecimal.ZERO);
        persistPO("PO-OD2", POStatus.PARTIAL,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), BigDecimal.ZERO);
        persistPO("PO-OD3", POStatus.DRAFT,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), BigDecimal.ZERO);
        persistPO("PO-OD4", POStatus.APPROVED,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1), BigDecimal.ZERO);

        List<PurchaseOrder> result = purchaseOrderRepository.findOverdueByTenantId(
                LocalDate.of(2024, 3, 1), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void searchByTenantId_matchesMultipleFields() {
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("SRCH-PO-001")
                .vendor(vendor)
                .location(location)
                .status(POStatus.DRAFT)
                .orderDate(LocalDate.of(2024, 1, 1))
                .vendorReference("VND-REF-123")
                .build();
        po.setTenantId(1L);
        em.persistAndFlush(po);

        Page<PurchaseOrder> byPoNumber = purchaseOrderRepository.searchByTenantId(
                1L, "SRCH-PO", PageRequest.of(0, 10));
        assertEquals(1, byPoNumber.getTotalElements());

        Page<PurchaseOrder> byVendorRef = purchaseOrderRepository.searchByTenantId(
                1L, "VND-REF", PageRequest.of(0, 10));
        assertEquals(1, byVendorRef.getTotalElements());
    }

    @Test
    void findMaxPoNumberByTenantId_returnsMaxNumber() {
        persistPO("PO-001", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);
        persistPO("PO-002", POStatus.DRAFT, LocalDate.of(2024, 1, 2), null, BigDecimal.ZERO);

        String maxNumber = purchaseOrderRepository.findMaxPoNumberByTenantId(1L);

        assertEquals("PO-002", maxNumber);
    }

    @Test
    void existsByPoNumber_existingNumber_returnsTrue() {
        persistPO("EX-PO", POStatus.DRAFT, LocalDate.of(2024, 1, 1), null, BigDecimal.ZERO);

        assertTrue(purchaseOrderRepository.existsByPoNumber("EX-PO"));
        assertFalse(purchaseOrderRepository.existsByPoNumber("NONEXISTENT"));
    }

    @Test
    void sumTotalByDateRange_sumsNonDraftNonCancelled() {
        persistPO("SUM-1", POStatus.APPROVED, LocalDate.of(2024, 1, 15),
                null, new BigDecimal("1000"));
        persistPO("SUM-2", POStatus.RECEIVED, LocalDate.of(2024, 2, 15),
                null, new BigDecimal("2000"));
        persistPO("SUM-3", POStatus.DRAFT, LocalDate.of(2024, 1, 20),
                null, new BigDecimal("500"));
        persistPO("SUM-4", POStatus.CANCELLED, LocalDate.of(2024, 1, 25),
                null, new BigDecimal("300"));

        BigDecimal sum = purchaseOrderRepository.sumTotalByDateRange(
                1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertEquals(0, new BigDecimal("3000").compareTo(sum));
    }
}
