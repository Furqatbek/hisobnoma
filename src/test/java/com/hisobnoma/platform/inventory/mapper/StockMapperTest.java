package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.StockDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StockMapperTest {

    @Autowired
    private StockMapper stockMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product = Product.builder()
                .id(5L)
                .sku("SKU-001")
                .name("Galaxy S24")
                .barcode("1234567890123")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("800.0000"))
                .reorderPoint(new BigDecimal("20.0000"))
                .minStockLevel(new BigDecimal("10.0000"))
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

        Stock stock = Stock.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .location(location)
                .quantityOnHand(new BigDecimal("100.0000"))
                .quantityReserved(new BigDecimal("10.0000"))
                .quantityInTransit(new BigDecimal("5.0000"))
                .quantityOnOrder(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("500.0000"))
                .lastCost(new BigDecimal("510.0000"))
                .reorderPoint(new BigDecimal("25.0000"))
                .reorderQuantity(new BigDecimal("100.0000"))
                .minStockLevel(new BigDecimal("15.0000"))
                .maxStockLevel(new BigDecimal("500.0000"))
                .build();

        // When
        StockDto dto = stockMapper.toDto(stock);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(5L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Galaxy S24", dto.getProductName());
        assertEquals("1234567890123", dto.getProductBarcode());
        assertEquals(3L, dto.getLocationId());
        assertEquals("WH-001", dto.getLocationCode());
        assertEquals("Main Warehouse", dto.getLocationName());
        assertEquals(new BigDecimal("100.0000"), dto.getQuantityOnHand());
        assertEquals(new BigDecimal("10.0000"), dto.getQuantityReserved());
        assertEquals(new BigDecimal("5.0000"), dto.getQuantityInTransit());
        assertEquals(new BigDecimal("50.0000"), dto.getQuantityOnOrder());
        assertEquals(new BigDecimal("500.0000"), dto.getAverageCost());
        assertEquals(new BigDecimal("510.0000"), dto.getLastCost());
        assertEquals(new BigDecimal("25.0000"), dto.getReorderPoint());
        assertEquals(new BigDecimal("100.0000"), dto.getReorderQuantity());
        assertEquals(new BigDecimal("15.0000"), dto.getMinStockLevel());
        assertEquals(new BigDecimal("500.0000"), dto.getMaxStockLevel());
        assertEquals("PCS", dto.getBaseUomCode());
        assertEquals("Pieces", dto.getBaseUomName());
        // Expression-based computed fields
        assertEquals(new BigDecimal("90.0000"), dto.getQuantityAvailable());
        assertEquals(0, new BigDecimal("50000.0000").compareTo(dto.getStockValue()));
        assertFalse(dto.isBelowReorderPoint());
        assertFalse(dto.isBelowMinimum());
    }

    @Test
    void toDto_belowReorderPoint() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product = Product.builder()
                .id(5L)
                .sku("SKU-001")
                .name("Product")
                .baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .reorderPoint(new BigDecimal("50.0000"))
                .minStockLevel(new BigDecimal("10.0000"))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        Location location = Location.builder()
                .id(3L)
                .code("WH-001")
                .name("Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .childLocations(new ArrayList<>())
                .build();

        Stock stock = Stock.builder()
                .id(2L)
                .product(product)
                .location(location)
                .quantityOnHand(new BigDecimal("5.0000"))
                .quantityReserved(BigDecimal.ZERO)
                .quantityInTransit(BigDecimal.ZERO)
                .quantityOnOrder(BigDecimal.ZERO)
                .averageCost(new BigDecimal("10.0000"))
                .build();

        // When
        StockDto dto = stockMapper.toDto(stock);

        // Then
        assertTrue(dto.isBelowReorderPoint());
        assertTrue(dto.isBelowMinimum());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder().id(1L).code("PCS").name("Pieces").build();
        Product product = Product.builder()
                .id(1L).sku("SKU-001").name("Product A").baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();
        Location location = Location.builder()
                .id(1L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        Stock stock1 = Stock.builder()
                .id(1L).product(product).location(location)
                .quantityOnHand(new BigDecimal("100")).quantityReserved(BigDecimal.ZERO)
                .quantityInTransit(BigDecimal.ZERO).quantityOnOrder(BigDecimal.ZERO)
                .averageCost(BigDecimal.TEN)
                .build();

        Stock stock2 = Stock.builder()
                .id(2L).product(product).location(location)
                .quantityOnHand(new BigDecimal("200")).quantityReserved(BigDecimal.ZERO)
                .quantityInTransit(BigDecimal.ZERO).quantityOnOrder(BigDecimal.ZERO)
                .averageCost(BigDecimal.TEN)
                .build();

        // When
        List<StockDto> dtos = stockMapper.toDtoList(List.of(stock1, stock2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
