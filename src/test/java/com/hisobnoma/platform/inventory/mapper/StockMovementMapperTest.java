package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.StockMovementDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StockMovementMapperTest {

    @Autowired
    private StockMovementMapper stockMovementMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(5L)
                .sku("SKU-001")
                .name("Galaxy S24")
                .baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(10L)
                .name("128GB Black")
                .product(product)
                .sku("SKU-001-BLK")
                .build();

        Location fromLocation = Location.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .childLocations(new ArrayList<>())
                .build();

        Location toLocation = Location.builder()
                .id(2L)
                .code("ST-001")
                .name("Store Downtown")
                .locationType(LocationType.STORE)
                .childLocations(new ArrayList<>())
                .build();

        LocalDateTime movementDate = LocalDateTime.of(2025, 1, 15, 10, 30);

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .productVariant(variant)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .movementType(MovementType.TRANSFER)
                .quantity(new BigDecimal("25.0000"))
                .unitCost(new BigDecimal("500.0000"))
                .totalCost(new BigDecimal("12500.0000"))
                .referenceType(MovementReferenceType.TRANSFER_ORDER)
                .referenceId(100L)
                .referenceNumber("TRF-001")
                .movementDate(movementDate)
                .reason("Replenish store inventory")
                .notes("Transfer approved by manager")
                .batchNumber("BATCH-001")
                .serialNumber("SN-001")
                .quantityBefore(new BigDecimal("200.0000"))
                .quantityAfter(new BigDecimal("175.0000"))
                .reversed(false)
                .reversalMovementId(null)
                .originalMovementId(null)
                .build();

        // When
        StockMovementDto dto = stockMovementMapper.toDto(movement);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(5L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Galaxy S24", dto.getProductName());
        assertEquals(10L, dto.getProductVariantId());
        assertEquals("128GB Black", dto.getVariantName());
        assertEquals(1L, dto.getFromLocationId());
        assertEquals("Main Warehouse", dto.getFromLocationName());
        assertEquals(2L, dto.getToLocationId());
        assertEquals("Store Downtown", dto.getToLocationName());
        assertEquals(MovementType.TRANSFER, dto.getMovementType());
        assertEquals(new BigDecimal("25.0000"), dto.getQuantity());
        assertEquals(new BigDecimal("500.0000"), dto.getUnitCost());
        assertEquals(new BigDecimal("12500.0000"), dto.getTotalCost());
        assertEquals(MovementReferenceType.TRANSFER_ORDER, dto.getReferenceType());
        assertEquals(100L, dto.getReferenceId());
        assertEquals("TRF-001", dto.getReferenceNumber());
        assertEquals(movementDate, dto.getMovementDate());
        assertEquals("Replenish store inventory", dto.getReason());
        assertEquals("Transfer approved by manager", dto.getNotes());
        assertEquals("BATCH-001", dto.getBatchNumber());
        assertEquals("SN-001", dto.getSerialNumber());
        assertEquals(new BigDecimal("200.0000"), dto.getQuantityBefore());
        assertEquals(new BigDecimal("175.0000"), dto.getQuantityAfter());
        assertFalse(dto.isReversed());
        assertNull(dto.getReversalMovementId());
        assertNull(dto.getOriginalMovementId());
    }

    @Test
    void toDto_withNullOptionalRelations() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(5L)
                .sku("SKU-001")
                .name("Product")
                .baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .product(product)
                .productVariant(null)
                .fromLocation(null)
                .toLocation(null)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(new BigDecimal("10.0000"))
                .movementDate(LocalDateTime.now())
                .build();

        // When
        StockMovementDto dto = stockMovementMapper.toDto(movement);

        // Then
        assertNotNull(dto);
        assertNull(dto.getProductVariantId());
        assertNull(dto.getVariantName());
        assertNull(dto.getFromLocationId());
        assertNull(dto.getFromLocationName());
        assertNull(dto.getToLocationId());
        assertNull(dto.getToLocationName());
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

        StockMovement m1 = StockMovement.builder()
                .id(1L).product(product).movementType(MovementType.STOCK_IN)
                .quantity(BigDecimal.TEN).movementDate(LocalDateTime.now())
                .build();

        StockMovement m2 = StockMovement.builder()
                .id(2L).product(product).movementType(MovementType.STOCK_OUT)
                .quantity(new BigDecimal("5")).movementDate(LocalDateTime.now())
                .build();

        // When
        List<StockMovementDto> dtos = stockMovementMapper.toDtoList(List.of(m1, m2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
