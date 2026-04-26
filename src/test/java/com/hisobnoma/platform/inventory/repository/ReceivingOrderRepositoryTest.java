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
class ReceivingOrderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReceivingOrderRepository receivingOrderRepository;

    private Vendor vendor;
    private Location location;
    private PurchaseOrder purchaseOrder;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .code("RO-V1").name("RO Vendor").active(true).build();
        vendor.setTenantId(1L);
        vendor = em.persistAndFlush(vendor);

        location = Location.builder()
                .code("RO-WH1").name("RO Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);

        purchaseOrder = PurchaseOrder.builder()
                .poNumber("RO-PO-001")
                .vendor(vendor)
                .location(location)
                .status(POStatus.APPROVED)
                .orderDate(LocalDate.of(2024, 1, 1))
                .build();
        purchaseOrder.setTenantId(1L);
        purchaseOrder = em.persistAndFlush(purchaseOrder);
    }

    private ReceivingOrder persistRO(String receivingNumber, ReceivingStatus status,
                                     LocalDate receivingDate, PurchaseOrder po) {
        ReceivingOrder ro = ReceivingOrder.builder()
                .receivingNumber(receivingNumber)
                .vendor(vendor)
                .location(location)
                .status(status)
                .receivingDate(receivingDate)
                .purchaseOrder(po)
                .build();
        ro.setTenantId(1L);
        return em.persistAndFlush(ro);
    }

    @Test
    void findByIdAndTenantId_existingRO_returnsRO() {
        ReceivingOrder ro = persistRO("RO-001", ReceivingStatus.DRAFT,
                LocalDate.of(2024, 1, 15), purchaseOrder);

        Optional<ReceivingOrder> result = receivingOrderRepository.findByIdAndTenantId(ro.getId(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findAllByTenantId_returnsAllForTenant() {
        persistRO("RO-A1", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), null);
        persistRO("RO-A2", ReceivingStatus.COMPLETED, LocalDate.of(2024, 1, 2), null);

        Page<ReceivingOrder> result = receivingOrderRepository.findAllByTenantId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByReceivingNumberAndTenantId_existingNumber_returnsRO() {
        persistRO("RO-FIND", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), null);

        Optional<ReceivingOrder> result = receivingOrderRepository
                .findByReceivingNumberAndTenantId("RO-FIND", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByStatusAndTenantId_filtersByStatus() {
        persistRO("RO-D1", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), null);
        persistRO("RO-C1", ReceivingStatus.COMPLETED, LocalDate.of(2024, 1, 2), null);

        Page<ReceivingOrder> result = receivingOrderRepository.findByStatusAndTenantId(
                ReceivingStatus.DRAFT, 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByPurchaseOrderIdAndTenantId_returnsRelated() {
        persistRO("RO-PO-R1", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), purchaseOrder);
        persistRO("RO-NO-PO", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 2), null);

        List<ReceivingOrder> result = receivingOrderRepository
                .findByPurchaseOrderIdAndTenantId(purchaseOrder.getId(), 1L);

        assertEquals(1, result.size());
    }

    @Test
    void findByVendorIdAndTenantId_filtersByVendor() {
        persistRO("RO-VND1", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), null);

        Page<ReceivingOrder> result = receivingOrderRepository.findByVendorIdAndTenantId(
                vendor.getId(), 1L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByDateRangeAndTenantId_filtersCorrectly() {
        persistRO("RO-DR1", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 15), null);
        persistRO("RO-DR2", ReceivingStatus.DRAFT, LocalDate.of(2024, 3, 15), null);
        persistRO("RO-DR3", ReceivingStatus.DRAFT, LocalDate.of(2024, 5, 15), null);

        Page<ReceivingOrder> result = receivingOrderRepository.findByDateRangeAndTenantId(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31), 1L, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchByTenantId_matchesMultipleFields() {
        ReceivingOrder ro = ReceivingOrder.builder()
                .receivingNumber("SRCH-RO-001")
                .vendor(vendor)
                .location(location)
                .status(ReceivingStatus.DRAFT)
                .receivingDate(LocalDate.of(2024, 1, 1))
                .vendorDeliveryNote("DN-123")
                .vendorInvoiceNumber("INV-456")
                .build();
        ro.setTenantId(1L);
        em.persistAndFlush(ro);

        Page<ReceivingOrder> byNumber = receivingOrderRepository.searchByTenantId(
                1L, "SRCH-RO", PageRequest.of(0, 10));
        assertEquals(1, byNumber.getTotalElements());

        Page<ReceivingOrder> byDN = receivingOrderRepository.searchByTenantId(
                1L, "DN-123", PageRequest.of(0, 10));
        assertEquals(1, byDN.getTotalElements());

        Page<ReceivingOrder> byInv = receivingOrderRepository.searchByTenantId(
                1L, "INV-456", PageRequest.of(0, 10));
        assertEquals(1, byInv.getTotalElements());
    }

    @Test
    void findMaxReceivingNumberByTenantId_returnsMaxNumber() {
        persistRO("RO-001", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), null);
        persistRO("RO-002", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 2), null);

        String maxNumber = receivingOrderRepository.findMaxReceivingNumberByTenantId(1L);

        assertEquals("RO-002", maxNumber);
    }

    @Test
    void existsByReceivingNumber_existingNumber_returnsTrue() {
        persistRO("EX-RO", ReceivingStatus.DRAFT, LocalDate.of(2024, 1, 1), null);

        assertTrue(receivingOrderRepository.existsByReceivingNumber("EX-RO"));
        assertFalse(receivingOrderRepository.existsByReceivingNumber("NONEXISTENT"));
    }
}
