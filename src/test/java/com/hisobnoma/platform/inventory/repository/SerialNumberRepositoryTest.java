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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class SerialNumberRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SerialNumberRepository serialNumberRepository;

    private Product product;
    private Location location;

    @BeforeEach
    void setUp() {
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        uom = em.persistAndFlush(uom);

        product = Product.builder()
                .sku("SN-P1").name("Serial Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUom(uom).build();
        product.setTenantId(1L);
        product = em.persistAndFlush(product);

        location = Location.builder()
                .code("SN-WH1").name("Serial Warehouse")
                .locationType(LocationType.WAREHOUSE).active(true).build();
        location.setTenantId(1L);
        location = em.persistAndFlush(location);
    }

    private SerialNumber persistSerial(String serial, SerialNumber.SerialStatus status,
                                       boolean reserved) {
        SerialNumber sn = SerialNumber.builder()
                .product(product)
                .location(location)
                .serialNumber(serial)
                .status(status)
                .reserved(reserved)
                .build();
        sn.setTenantId(1L);
        return em.persistAndFlush(sn);
    }

    @Test
    void findByProductIdAndSerialNumberAndTenantId_existingSerial_returnsSerial() {
        persistSerial("SN-001", SerialNumber.SerialStatus.AVAILABLE, false);

        Optional<SerialNumber> result = serialNumberRepository
                .findByProductIdAndSerialNumberAndTenantId(product.getId(), "SN-001", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findBySerialNumberAndTenantId_existingSerial_returnsSerial() {
        persistSerial("SN-FIND", SerialNumber.SerialStatus.AVAILABLE, false);

        Optional<SerialNumber> result = serialNumberRepository
                .findBySerialNumberAndTenantId("SN-FIND", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByImeiAndTenantId_existingImei_returnsSerial() {
        SerialNumber sn = SerialNumber.builder()
                .product(product)
                .location(location)
                .serialNumber("SN-IMEI")
                .imei("123456789012345")
                .status(SerialNumber.SerialStatus.AVAILABLE)
                .build();
        sn.setTenantId(1L);
        em.persistAndFlush(sn);

        Optional<SerialNumber> result = serialNumberRepository.findByImeiAndTenantId("123456789012345", 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void findByProductIdAndTenantId_returnsAllSerialsForProduct() {
        persistSerial("SN-A", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("SN-B", SerialNumber.SerialStatus.SOLD, false);

        List<SerialNumber> result = serialNumberRepository.findByProductIdAndTenantId(product.getId(), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void findByProductIdAndLocationIdAndTenantId_returnsAtLocation() {
        persistSerial("SN-LOC", SerialNumber.SerialStatus.AVAILABLE, false);

        List<SerialNumber> result = serialNumberRepository
                .findByProductIdAndLocationIdAndTenantId(product.getId(), location.getId(), 1L);

        assertEquals(1, result.size());
    }

    @Test
    void findByProductIdAndLocationIdAndStatusAndTenantId_filtersByStatus() {
        persistSerial("SN-AVAIL", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("SN-SOLD", SerialNumber.SerialStatus.SOLD, false);

        List<SerialNumber> result = serialNumberRepository
                .findByProductIdAndLocationIdAndStatusAndTenantId(
                        product.getId(), location.getId(), SerialNumber.SerialStatus.AVAILABLE, 1L);

        assertEquals(1, result.size());
        assertEquals("SN-AVAIL", result.get(0).getSerialNumber());
    }

    @Test
    void findAvailableSerials_returnsAvailableAndNotReserved() {
        persistSerial("SN-FREE", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("SN-RESV", SerialNumber.SerialStatus.AVAILABLE, true);
        persistSerial("SN-SOLD2", SerialNumber.SerialStatus.SOLD, false);

        List<SerialNumber> result = serialNumberRepository.findAvailableSerials(
                1L, product.getId(), location.getId());

        assertEquals(1, result.size());
        assertEquals("SN-FREE", result.get(0).getSerialNumber());
    }

    @Test
    void findByTenantIdAndStatus_paged_filtersByStatus() {
        persistSerial("SN-STAT1", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("SN-STAT2", SerialNumber.SerialStatus.SOLD, false);

        Page<SerialNumber> result = serialNumberRepository.findByTenantIdAndStatus(
                1L, SerialNumber.SerialStatus.AVAILABLE, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchSerials_matchesMultipleFields() {
        SerialNumber sn = SerialNumber.builder()
                .product(product)
                .location(location)
                .serialNumber("SRCH-SN-001")
                .imei("SRCH-IMEI-001")
                .macAddress("AA:BB:CC:DD:EE:FF")
                .status(SerialNumber.SerialStatus.AVAILABLE)
                .build();
        sn.setTenantId(1L);
        em.persistAndFlush(sn);

        Page<SerialNumber> bySerial = serialNumberRepository.searchSerials(
                1L, "SRCH-SN", PageRequest.of(0, 10));
        assertEquals(1, bySerial.getTotalElements());

        Page<SerialNumber> byImei = serialNumberRepository.searchSerials(
                1L, "SRCH-IMEI", PageRequest.of(0, 10));
        assertEquals(1, byImei.getTotalElements());

        Page<SerialNumber> byMac = serialNumberRepository.searchSerials(
                1L, "AA:BB:CC", PageRequest.of(0, 10));
        assertEquals(1, byMac.getTotalElements());
    }

    @Test
    void existsByProductIdAndSerialNumberAndTenantId_existing_returnsTrue() {
        persistSerial("EX-SN", SerialNumber.SerialStatus.AVAILABLE, false);

        assertTrue(serialNumberRepository.existsByProductIdAndSerialNumberAndTenantId(
                product.getId(), "EX-SN", 1L));
        assertFalse(serialNumberRepository.existsByProductIdAndSerialNumberAndTenantId(
                product.getId(), "NONEXISTENT", 1L));
    }

    @Test
    void existsByImeiAndTenantId_existing_returnsTrue() {
        SerialNumber sn = SerialNumber.builder()
                .product(product)
                .location(location)
                .serialNumber("SN-IMEI2")
                .imei("IMEI-EXISTS")
                .status(SerialNumber.SerialStatus.AVAILABLE)
                .build();
        sn.setTenantId(1L);
        em.persistAndFlush(sn);

        assertTrue(serialNumberRepository.existsByImeiAndTenantId("IMEI-EXISTS", 1L));
    }

    @Test
    void countAvailableByProduct_countsCorrectly() {
        persistSerial("CNT-1", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("CNT-2", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("CNT-3", SerialNumber.SerialStatus.SOLD, false);

        Long count = serialNumberRepository.countAvailableByProduct(1L, product.getId());

        assertEquals(2, count);
    }

    @Test
    void findBySerialNumberInAndTenantId_returnsMatchingSerials() {
        persistSerial("IN-1", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("IN-2", SerialNumber.SerialStatus.AVAILABLE, false);
        persistSerial("IN-3", SerialNumber.SerialStatus.AVAILABLE, false);

        List<SerialNumber> result = serialNumberRepository
                .findBySerialNumberInAndTenantId(List.of("IN-1", "IN-3"), 1L);

        assertEquals(2, result.size());
    }

    @Test
    void findByCustomer_returnsSerialsSoldToCustomer() {
        SerialNumber sn = SerialNumber.builder()
                .product(product)
                .location(location)
                .serialNumber("CUST-SN")
                .status(SerialNumber.SerialStatus.SOLD)
                .customerId(42L)
                .soldDate(java.time.LocalDateTime.now())
                .build();
        sn.setTenantId(1L);
        em.persistAndFlush(sn);

        Page<SerialNumber> result = serialNumberRepository.findByCustomer(1L, 42L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }
}
