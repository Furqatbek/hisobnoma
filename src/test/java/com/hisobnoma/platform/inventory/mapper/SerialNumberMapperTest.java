package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.SerialNumberDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SerialNumberMapperTest {

    @Autowired
    private SerialNumberMapper serialNumberMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L)
                .sku("SKU-001")
                .name("Galaxy S24")
                .baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(20L)
                .name("128GB Black")
                .product(product)
                .sku("SKU-001-BLK")
                .build();

        Location location = Location.builder()
                .id(3L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .childLocations(new ArrayList<>())
                .build();

        StockBatch batch = StockBatch.builder()
                .id(50L)
                .batchNumber("BATCH-2025-001")
                .product(product)
                .location(location)
                .build();

        LocalDate warrantyEnd = LocalDate.now().plusYears(2);

        SerialNumber serial = SerialNumber.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .productVariant(variant)
                .location(location)
                .serialNumber("SN-2025-00001")
                .status(SerialNumber.SerialStatus.AVAILABLE)
                .batch(batch)
                .unitCost(new BigDecimal("500.0000"))
                .manufactureDate(LocalDate.of(2024, 12, 1))
                .expiryDate(null)
                .warrantyStartDate(LocalDate.of(2025, 1, 15))
                .warrantyEndDate(warrantyEnd)
                .receivedDate(LocalDateTime.of(2025, 1, 15, 10, 0))
                .soldDate(null)
                .supplierId(100L)
                .customerId(null)
                .receivingOrderId(200L)
                .salesTransactionId(null)
                .manufacturerSerial("SAM-SN-12345")
                .imei("123456789012345")
                .macAddress("AA:BB:CC:DD:EE:FF")
                .notes("New unit, unopened")
                .reserved(false)
                .reservedForOrderId(null)
                .build();

        // When
        SerialNumberDto dto = serialNumberMapper.toDto(serial);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Galaxy S24", dto.getProductName());
        assertEquals(20L, dto.getProductVariantId());
        assertEquals("128GB Black", dto.getVariantName());
        assertEquals(3L, dto.getLocationId());
        assertEquals("WH-001", dto.getLocationCode());
        assertEquals("Main Warehouse", dto.getLocationName());
        assertEquals("SN-2025-00001", dto.getSerialNumber());
        assertEquals(SerialNumber.SerialStatus.AVAILABLE, dto.getStatus());
        assertEquals(50L, dto.getBatchId());
        assertEquals("BATCH-2025-001", dto.getBatchNumber());
        assertEquals(new BigDecimal("500.0000"), dto.getUnitCost());
        assertEquals(LocalDate.of(2024, 12, 1), dto.getManufactureDate());
        assertNull(dto.getExpiryDate());
        assertEquals(LocalDate.of(2025, 1, 15), dto.getWarrantyStartDate());
        assertEquals(warrantyEnd, dto.getWarrantyEndDate());
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 0), dto.getReceivedDate());
        assertNull(dto.getSoldDate());
        assertEquals(100L, dto.getSupplierId());
        assertNull(dto.getCustomerId());
        assertEquals(200L, dto.getReceivingOrderId());
        assertNull(dto.getSalesTransactionId());
        assertEquals("SAM-SN-12345", dto.getManufacturerSerial());
        assertEquals("123456789012345", dto.getImei());
        assertEquals("AA:BB:CC:DD:EE:FF", dto.getMacAddress());
        assertEquals("New unit, unopened", dto.getNotes());
        assertFalse(dto.isReserved());
        assertNull(dto.getReservedForOrderId());
        // Expression-based
        assertTrue(dto.isUnderWarranty());
        assertTrue(dto.getWarrantyDaysRemaining() > 0);
    }

    @Test
    void toDto_withNullOptionalRelations() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L).sku("SKU-001").name("Product")
                .baseUom(uom).sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        SerialNumber serial = SerialNumber.builder()
                .id(1L)
                .product(product)
                .productVariant(null)
                .location(null)
                .serialNumber("SN-001")
                .status(SerialNumber.SerialStatus.AVAILABLE)
                .batch(null)
                .warrantyEndDate(null)
                .build();

        // When
        SerialNumberDto dto = serialNumberMapper.toDto(serial);

        // Then
        assertNotNull(dto);
        assertNull(dto.getProductVariantId());
        assertNull(dto.getVariantName());
        assertNull(dto.getLocationId());
        assertNull(dto.getLocationCode());
        assertNull(dto.getLocationName());
        assertNull(dto.getBatchId());
        assertNull(dto.getBatchNumber());
        assertFalse(dto.isUnderWarranty());
        assertEquals(0L, dto.getWarrantyDaysRemaining());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();
        Product product = Product.builder()
                .id(1L).sku("SKU-001").name("Product").baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        SerialNumber sn1 = SerialNumber.builder()
                .id(1L).product(product).serialNumber("SN-001")
                .status(SerialNumber.SerialStatus.AVAILABLE).build();
        SerialNumber sn2 = SerialNumber.builder()
                .id(2L).product(product).serialNumber("SN-002")
                .status(SerialNumber.SerialStatus.SOLD).build();

        // When
        List<SerialNumberDto> dtos = serialNumberMapper.toDtoList(List.of(sn1, sn2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
