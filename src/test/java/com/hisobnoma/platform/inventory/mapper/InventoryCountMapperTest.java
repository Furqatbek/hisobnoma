package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.InventoryCountDto;
import com.hisobnoma.platform.inventory.dto.InventoryCountLineDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InventoryCountMapperTest {

    @Autowired
    private InventoryCountMapper inventoryCountMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Location location = Location.builder()
                .id(3L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .childLocations(new ArrayList<>())
                .build();

        Category category = Category.builder()
                .id(5L)
                .name("Electronics")
                .children(new ArrayList<>())
                .build();

        InventoryCount ic = InventoryCount.builder()
                .id(1L)
                .tenantId(1L)
                .countNumber("IC-2025-001")
                .location(location)
                .status(CountStatus.IN_PROGRESS)
                .countType(CountType.FULL)
                .countDate(LocalDate.of(2025, 3, 15))
                .description("Full count for warehouse")
                .notes("Monthly count")
                .totalItems(100)
                .countedItems(60)
                .varianceItems(5)
                .totalVarianceValue(new BigDecimal("1500.0000"))
                .positiveVarianceValue(new BigDecimal("2000.0000"))
                .negativeVarianceValue(new BigDecimal("500.0000"))
                .freezeStock(true)
                .startedAt(Instant.parse("2025-03-15T08:00:00Z"))
                .completedAt(null)
                .adjustmentsPosted(false)
                .category(category)
                .lines(new ArrayList<>())
                .build();

        // When
        InventoryCountDto dto = inventoryCountMapper.toDto(ic);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("IC-2025-001", dto.getCountNumber());
        assertEquals(3L, dto.getLocationId());
        assertEquals("WH-001", dto.getLocationCode());
        assertEquals("Main Warehouse", dto.getLocationName());
        assertEquals(CountStatus.IN_PROGRESS, dto.getStatus());
        assertEquals(CountType.FULL, dto.getCountType());
        assertEquals(LocalDate.of(2025, 3, 15), dto.getCountDate());
        assertEquals("Full count for warehouse", dto.getDescription());
        assertEquals("Monthly count", dto.getNotes());
        assertEquals(100, dto.getTotalItems());
        assertEquals(60, dto.getCountedItems());
        assertEquals(5, dto.getVarianceItems());
        assertEquals(new BigDecimal("1500.0000"), dto.getTotalVarianceValue());
        assertEquals(new BigDecimal("2000.0000"), dto.getPositiveVarianceValue());
        assertEquals(new BigDecimal("500.0000"), dto.getNegativeVarianceValue());
        assertTrue(dto.isFreezeStock());
        assertEquals(Instant.parse("2025-03-15T08:00:00Z"), dto.getStartedAt());
        assertNull(dto.getCompletedAt());
        assertFalse(dto.isAdjustmentsPosted());
        assertEquals(5L, dto.getCategoryId());
        assertEquals("Electronics", dto.getCategoryName());
        // Expression-based
        assertNotNull(dto.getCompletionPercentage());
        assertFalse(dto.isComplete());
    }

    @Test
    void toDto_withNullCategory() {
        // Given
        Location location = Location.builder()
                .id(3L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        InventoryCount ic = InventoryCount.builder()
                .id(1L)
                .countNumber("IC-002")
                .location(location)
                .status(CountStatus.DRAFT)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.now())
                .category(null)
                .lines(new ArrayList<>())
                .build();

        // When
        InventoryCountDto dto = inventoryCountMapper.toDto(ic);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCategoryId());
        assertNull(dto.getCategoryName());
    }

    @Test
    void toLineDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L).sku("SKU-001").name("Galaxy S24")
                .baseUom(uom).sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(20L).name("128GB Black").product(product).sku("SKU-001-BLK").build();

        InventoryCount ic = InventoryCount.builder()
                .id(1L).countNumber("IC-001").lines(new ArrayList<>()).build();

        Instant countedAt = Instant.parse("2025-03-15T10:00:00Z");

        InventoryCountLine line = InventoryCountLine.builder()
                .id(100L)
                .inventoryCount(ic)
                .product(product)
                .productVariant(variant)
                .lineNumber(1)
                .systemQuantity(new BigDecimal("50.0000"))
                .countedQuantity(new BigDecimal("48.0000"))
                .varianceQuantity(new BigDecimal("-2.0000"))
                .unitCost(new BigDecimal("500.0000"))
                .varianceValue(new BigDecimal("-1000.0000"))
                .uom(uom)
                .batchNumber("BATCH-001")
                .serialNumber("SN-001")
                .counted(true)
                .countedBy(10L)
                .countedAt(countedAt)
                .recountRequired(false)
                .notes("Two units missing")
                .adjustmentReason("Theft suspected")
                .stockMovementId(200L)
                .adjustmentApproved(false)
                .build();

        // When
        InventoryCountLineDto dto = inventoryCountMapper.toLineDto(line);

        // Then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getInventoryCountId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductSku());
        assertEquals("Galaxy S24", dto.getProductName());
        assertEquals(20L, dto.getProductVariantId());
        assertEquals("128GB Black", dto.getVariantName());
        assertEquals(1, dto.getLineNumber());
        assertEquals(new BigDecimal("50.0000"), dto.getSystemQuantity());
        assertEquals(new BigDecimal("48.0000"), dto.getCountedQuantity());
        assertEquals(new BigDecimal("-2.0000"), dto.getVarianceQuantity());
        assertEquals(new BigDecimal("500.0000"), dto.getUnitCost());
        assertEquals(new BigDecimal("-1000.0000"), dto.getVarianceValue());
        assertEquals(1L, dto.getUomId());
        assertEquals("PCS", dto.getUomCode());
        assertEquals("Pieces", dto.getUomName());
        assertEquals("BATCH-001", dto.getBatchNumber());
        assertEquals("SN-001", dto.getSerialNumber());
        assertTrue(dto.isCounted());
        assertEquals(10L, dto.getCountedBy());
        assertEquals(countedAt, dto.getCountedAt());
        assertFalse(dto.isRecountRequired());
        assertEquals("Two units missing", dto.getNotes());
        assertEquals("Theft suspected", dto.getAdjustmentReason());
        assertEquals(200L, dto.getStockMovementId());
        assertFalse(dto.isAdjustmentApproved());
        // Expression-based
        assertTrue(dto.isHasVariance());
        assertFalse(dto.isPositiveVariance());
        assertTrue(dto.isNegativeVariance());
        assertNotNull(dto.getVariancePercentage());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Location location = Location.builder()
                .id(1L).code("WH-001").name("Warehouse")
                .locationType(LocationType.WAREHOUSE).childLocations(new ArrayList<>())
                .build();

        InventoryCount ic1 = InventoryCount.builder()
                .id(1L).countNumber("IC-001").location(location)
                .status(CountStatus.DRAFT).countType(CountType.FULL)
                .countDate(LocalDate.now()).lines(new ArrayList<>())
                .build();

        InventoryCount ic2 = InventoryCount.builder()
                .id(2L).countNumber("IC-002").location(location)
                .status(CountStatus.COMPLETED).countType(CountType.SPOT)
                .countDate(LocalDate.now()).lines(new ArrayList<>())
                .build();

        // When
        List<InventoryCountDto> dtos = inventoryCountMapper.toDtoList(List.of(ic1, ic2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("IC-001", dtos.get(0).getCountNumber());
        assertEquals("IC-002", dtos.get(1).getCountNumber());
    }
}
