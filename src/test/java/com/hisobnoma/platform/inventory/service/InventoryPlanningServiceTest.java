package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.inventory.dto.AbcAnalysisDto;
import com.hisobnoma.platform.inventory.dto.LowStockDto;
import com.hisobnoma.platform.inventory.dto.ReorderSuggestionDto;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockMovementRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryPlanningServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private InventoryPlanningService inventoryPlanningService;

    private static final Long TENANT_ID = 1L;
    private Product product;
    private Location location;
    private Stock stock;
    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .tenantId(TENANT_ID)
                .build();

        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PC")
                .name("Piece")
                .tenantId(TENANT_ID)
                .build();

        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .category(category)
                .baseUom(uom)
                .costPrice(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("150.00"))
                .active(true)
                .trackInventory(true)
                .reorderPoint(new BigDecimal("20"))
                .reorderQuantity(new BigDecimal("50"))
                .minStockLevel(new BigDecimal("5"))
                .maxStockLevel(new BigDecimal("100"))
                .tenantId(TENANT_ID)
                .build();

        location = Location.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .tenantId(TENANT_ID)
                .build();

        stock = Stock.builder()
                .id(1L)
                .product(product)
                .location(location)
                .quantityOnHand(new BigDecimal("10"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("100.00"))
                .tenantId(TENANT_ID)
                .build();
    }

    // ==================== getReorderSuggestions ====================

    @Test
    void getReorderSuggestions_withLocation_returnsSuggestions() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProductId());
        assertEquals("PROD-001", result.get(0).getProductSku());
        verify(stockRepository).findByLocationIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void getReorderSuggestions_withoutLocation_returnsSuggestions() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stockRepository).findByTenantId(TENANT_ID);
    }

    @Test
    void getReorderSuggestions_stockAboveReorderPoint_returnsEmpty() {
        // Given
        stock.setQuantityOnHand(new BigDecimal("50"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReorderSuggestions_noReorderPointSet_returnsEmpty() {
        // Given
        product.setReorderPoint(BigDecimal.ZERO);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReorderSuggestions_noTrackInventory_returnsEmpty() {
        // Given
        product.setTrackInventory(false);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReorderSuggestions_criticalPriority_whenBelowMinStock() {
        // Given
        stock.setQuantityOnHand(new BigDecimal("3"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CRITICAL", result.get(0).getPriority());
    }

    @Test
    void getReorderSuggestions_usesReorderQuantity_whenSet() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("50"), result.get(0).getSuggestedQuantity());
    }

    @Test
    void getReorderSuggestions_emptyStocks_returnsEmpty() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        List<ReorderSuggestionDto> result = inventoryPlanningService.getReorderSuggestions(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== performAbcAnalysis ====================

    @Test
    void performAbcAnalysis_returnsClassifiedProducts() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.sumMovementValueByProductAndDateRange(
                eq(1L), eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("5000.00"));
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));

        // When
        List<AbcAnalysisDto> result = inventoryPlanningService.performAbcAnalysis(90);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getClassification());
        assertNotNull(result.get(0).getPercentOfTotal());
        assertNotNull(result.get(0).getCumulativePercent());
        assertEquals("PROD-001", result.get(0).getProductSku());
        assertEquals("Test Product", result.get(0).getProductName());
        assertTrue(result.get(0).getTotalValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void performAbcAnalysis_noProducts_returnsEmpty() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(emptyPage);

        // When
        List<AbcAnalysisDto> result = inventoryPlanningService.performAbcAnalysis(90);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void performAbcAnalysis_inactiveProducts_skipped() {
        // Given
        product.setActive(false);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);

        // When
        List<AbcAnalysisDto> result = inventoryPlanningService.performAbcAnalysis(90);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void performAbcAnalysis_nullMovementValue_treatedAsZero() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.sumMovementValueByProductAndDateRange(
                eq(1L), eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(null);
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));

        // When
        List<AbcAnalysisDto> result = inventoryPlanningService.performAbcAnalysis(90);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).getMovementValue());
    }

    // ==================== getSlowMovingProducts ====================

    @Test
    void getSlowMovingProducts_returnsSlowMovers() {
        // Given
        Instant oldDate = Instant.now().minus(100, ChronoUnit.DAYS);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(oldDate);
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));

        // When
        List<LowStockDto> result = inventoryPlanningService.getSlowMovingProducts(30, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DISCOUNT", result.get(0).getRecommendation());
        assertTrue(result.get(0).getDaysSinceLastSale() >= 100);
    }

    @Test
    void getSlowMovingProducts_withLocation_filtersCorrectly() {
        // Given
        Instant oldDate = Instant.now().minus(60, ChronoUnit.DAYS);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(oldDate);
        when(stockRepository.findByProductIdAndLocationId(1L, 1L)).thenReturn(List.of(stock));

        // When
        List<LowStockDto> result = inventoryPlanningService.getSlowMovingProducts(30, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stockRepository).findByProductIdAndLocationId(1L, 1L);
    }

    @Test
    void getSlowMovingProducts_recentMovement_returnsEmpty() {
        // Given
        Instant recentDate = Instant.now().minus(5, ChronoUnit.DAYS);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(recentDate);

        // When
        List<LowStockDto> result = inventoryPlanningService.getSlowMovingProducts(30, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSlowMovingProducts_noMovement_included() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(null);
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));

        // When
        List<LowStockDto> result = inventoryPlanningService.getSlowMovingProducts(30, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(30, result.get(0).getDaysSinceLastSale());
    }

    @Test
    void getSlowMovingProducts_zeroStock_excluded() {
        // Given
        stock.setQuantityOnHand(BigDecimal.ZERO);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(null);
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));

        // When
        List<LowStockDto> result = inventoryPlanningService.getSlowMovingProducts(30, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getDeadStock ====================

    @Test
    void getDeadStock_returnsDeadStockProducts() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.existsByProductIdAndTenantIdAndCreatedAtAfter(
                eq(1L), eq(TENANT_ID), any(Instant.class))).thenReturn(false);
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(null);

        // When
        List<LowStockDto> result = inventoryPlanningService.getDeadStock(90, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WRITE_OFF", result.get(0).getRecommendation());
    }

    @Test
    void getDeadStock_withLocation_filtersCorrectly() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.existsByProductIdAndTenantIdAndCreatedAtAfter(
                eq(1L), eq(TENANT_ID), any(Instant.class))).thenReturn(false);
        when(stockRepository.findByProductIdAndLocationId(1L, 1L)).thenReturn(List.of(stock));
        when(movementRepository.findLastMovementDateByProduct(1L, TENANT_ID)).thenReturn(null);

        // When
        List<LowStockDto> result = inventoryPlanningService.getDeadStock(90, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stockRepository).findByProductIdAndLocationId(1L, 1L);
    }

    @Test
    void getDeadStock_hasRecentMovement_excludesProduct() {
        // Given
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.existsByProductIdAndTenantIdAndCreatedAtAfter(
                eq(1L), eq(TENANT_ID), any(Instant.class))).thenReturn(true);

        // When
        List<LowStockDto> result = inventoryPlanningService.getDeadStock(90, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getDeadStock_zeroStock_excluded() {
        // Given
        stock.setQuantityOnHand(BigDecimal.ZERO);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);
        when(movementRepository.existsByProductIdAndTenantIdAndCreatedAtAfter(
                eq(1L), eq(TENANT_ID), any(Instant.class))).thenReturn(false);
        when(stockRepository.findByProductId(1L)).thenReturn(List.of(stock));

        // When
        List<LowStockDto> result = inventoryPlanningService.getDeadStock(90, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getDeadStock_inactiveProduct_skipped() {
        // Given
        product.setActive(false);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(productPage);

        // When
        List<LowStockDto> result = inventoryPlanningService.getDeadStock(90, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
