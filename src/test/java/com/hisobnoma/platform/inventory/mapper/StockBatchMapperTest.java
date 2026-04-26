package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.StockBatchDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StockBatchMapperTest {

    @Autowired
    private StockBatchMapper stockBatchMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L)
                .sku("SKU-001")
                .name("Aspirin 500mg")
                .baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        Location location = Location.builder()
                .id(3L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .childLocations(new ArrayList<>())
                .build();

        StockBatch batch = StockBatch.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .location(location)
                .batchNumber("BATCH-2025-001")
                .quantity(new BigDecimal("500.0000"))
                .initialQuantity(new BigDecimal("1000.0000"))
                .quantityReserved(new BigDecimal("50.0000"))
                .manufactureDate(LocalDate.of(2024, 6, 1))
                .expiryDate(LocalDate.now().plusYears(1))
                .receivedDate(LocalDate.of(2024, 7, 15))
                .supplierBatchNumber("SUP-BATCH-001")
                .unitCost(new BigDecimal("10.0000"))
                .status(StockBatch.BatchStatus.ACTIVE)
                .notes("From supplier delivery")
                .supplierId(100L)
                .receivingOrderId(200L)
                .build();

        // When
        StockBatchDto dto = stockBatchMapper.toDto(batch);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Aspirin 500mg", dto.getProductName());
        assertEquals(3L, dto.getLocationId());
        assertEquals("WH-001", dto.getLocationCode());
        assertEquals("Main Warehouse", dto.getLocationName());
        assertEquals("BATCH-2025-001", dto.getBatchNumber());
        assertEquals(new BigDecimal("500.0000"), dto.getQuantity());
        assertEquals(new BigDecimal("1000.0000"), dto.getInitialQuantity());
        assertEquals(new BigDecimal("50.0000"), dto.getQuantityReserved());
        assertEquals(LocalDate.of(2024, 6, 1), dto.getManufactureDate());
        assertNotNull(dto.getExpiryDate());
        assertEquals(LocalDate.of(2024, 7, 15), dto.getReceivedDate());
        assertEquals("SUP-BATCH-001", dto.getSupplierBatchNumber());
        assertEquals(new BigDecimal("10.0000"), dto.getUnitCost());
        assertEquals(StockBatch.BatchStatus.ACTIVE, dto.getStatus());
        assertEquals("From supplier delivery", dto.getNotes());
        assertEquals(100L, dto.getSupplierId());
        assertEquals(200L, dto.getReceivingOrderId());
        // Expression-based
        assertEquals(new BigDecimal("450.0000"), dto.getQuantityAvailable());
        assertEquals(0, new BigDecimal("5000.0000").compareTo(dto.getValue()));
        assertNotNull(dto.getDaysUntilExpiry());
        assertTrue(dto.getDaysUntilExpiry() > 0);
        assertFalse(dto.isExpired());
        assertFalse(dto.isExpiringWithin30Days());
    }

    @Test
    void toDto_expiredBatch() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L).sku("SKU-001").name("Expired Product")
                .baseUom(uom).sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        Location location = Location.builder()
                .id(3L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        StockBatch batch = StockBatch.builder()
                .id(2L)
                .product(product)
                .location(location)
                .batchNumber("BATCH-OLD-001")
                .quantity(new BigDecimal("100.0000"))
                .quantityReserved(BigDecimal.ZERO)
                .expiryDate(LocalDate.now().minusDays(10))
                .unitCost(new BigDecimal("5.0000"))
                .status(StockBatch.BatchStatus.EXPIRED)
                .build();

        // When
        StockBatchDto dto = stockBatchMapper.toDto(batch);

        // Then
        assertTrue(dto.isExpired());
        assertTrue(dto.isExpiringWithin30Days());
    }

    @Test
    void toDto_batchExpiringWithin30Days() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L).sku("SKU-001").name("Product")
                .baseUom(uom).sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        Location location = Location.builder()
                .id(3L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        StockBatch batch = StockBatch.builder()
                .id(3L)
                .product(product)
                .location(location)
                .batchNumber("BATCH-EXPIRING")
                .quantity(new BigDecimal("200.0000"))
                .quantityReserved(BigDecimal.ZERO)
                .expiryDate(LocalDate.now().plusDays(15))
                .unitCost(new BigDecimal("8.0000"))
                .status(StockBatch.BatchStatus.ACTIVE)
                .build();

        // When
        StockBatchDto dto = stockBatchMapper.toDto(batch);

        // Then
        assertFalse(dto.isExpired());
        assertTrue(dto.isExpiringWithin30Days());
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
        Location location = Location.builder()
                .id(1L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        StockBatch b1 = StockBatch.builder()
                .id(1L).product(product).location(location)
                .batchNumber("B-001").quantity(BigDecimal.TEN)
                .quantityReserved(BigDecimal.ZERO).build();
        StockBatch b2 = StockBatch.builder()
                .id(2L).product(product).location(location)
                .batchNumber("B-002").quantity(new BigDecimal("20"))
                .quantityReserved(BigDecimal.ZERO).build();

        // When
        List<StockBatchDto> dtos = stockBatchMapper.toDtoList(List.of(b1, b2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("B-001", dtos.get(0).getBatchNumber());
        assertEquals("B-002", dtos.get(1).getBatchNumber());
    }
}
