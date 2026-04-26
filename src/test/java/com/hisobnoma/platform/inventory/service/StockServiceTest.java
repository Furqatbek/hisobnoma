package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.event.InventoryEventPublisher;
import com.hisobnoma.platform.inventory.mapper.StockMapper;
import com.hisobnoma.platform.inventory.mapper.StockMovementMapper;
import com.hisobnoma.platform.inventory.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private StockBatchRepository stockBatchRepository;
    @Mock
    private SerialNumberRepository serialNumberRepository;
    @Mock
    private StockReservationRepository stockReservationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private StockMapper stockMapper;
    @Mock
    private StockMovementMapper stockMovementMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private InventoryEventPublisher inventoryEventPublisher;

    @InjectMocks
    private StockService stockService;

    private static final Long TENANT_ID = 1L;
    private Product product;
    private Location location;
    private Location location2;
    private Stock stock;
    private StockDto stockDto;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .costPrice(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("20.00"))
                .reorderPoint(new BigDecimal("5.00"))
                .reorderQuantity(new BigDecimal("20.00"))
                .tenantId(TENANT_ID)
                .build();

        location = Location.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        location2 = Location.builder()
                .id(2L)
                .code("WH-002")
                .name("Secondary Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        stock = Stock.builder()
                .id(1L)
                .product(product)
                .location(location)
                .quantityOnHand(new BigDecimal("50"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("10.00"))
                .tenantId(TENANT_ID)
                .build();

        stockDto = StockDto.builder()
                .id(1L)
                .productId(1L)
                .productSku("PROD-001")
                .locationId(1L)
                .quantityOnHand(new BigDecimal("50"))
                .quantityReserved(BigDecimal.ZERO)
                .quantityAvailable(new BigDecimal("50"))
                .averageCost(new BigDecimal("10.00"))
                .build();
    }

    // ==================== getStock ====================

    @Test
    void getStock_returnsPageOfAllStockRecords() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Stock> page = new PageImpl<>(List.of(stock), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(stockMapper.toDto(stock)).thenReturn(stockDto);

        // When
        PageResponse<StockDto> result = stockService.getStock(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getStock_returnsEmptyPage_whenNoStockExists() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Stock> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<StockDto> result = stockService.getStock(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getStockByProduct ====================

    @Test
    void getStockByProduct_returnsAllLocationsForProduct() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByProductIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(stock));
        when(stockMapper.toDto(stock)).thenReturn(stockDto);

        // When
        List<StockDto> result = stockService.getStockByProduct(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getStockByProduct_returnsEmptyList_whenNoStock() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByProductIdAndTenantId(1L, TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        List<StockDto> result = stockService.getStockByProduct(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getStockByLocation ====================

    @Test
    void getStockByLocation_returnsAllProductsInLocation() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Stock> page = new PageImpl<>(List.of(stock), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(stockMapper.toDto(stock)).thenReturn(stockDto);

        // When
        PageResponse<StockDto> result = stockService.getStockByLocation(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== getStockByProductAndLocation ====================

    @Test
    void getStockByProductAndLocation_returnsDto_whenFound() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByProductIdAndLocationIdAndTenantId(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stockMapper.toDto(stock)).thenReturn(stockDto);

        // When
        StockDto result = stockService.getStockByProductAndLocation(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("50"), result.getQuantityOnHand());
    }

    @Test
    void getStockByProductAndLocation_returnsNull_whenNoRecord() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.findByProductIdAndLocationIdAndTenantId(1L, 1L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When
        StockDto result = stockService.getStockByProductAndLocation(1L, 1L);

        // Then
        assertNull(result);
    }

    // ==================== getStockValuation ====================

    @Test
    void getStockValuation_returnsValue() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getTotalStockValue(TENANT_ID)).thenReturn(new BigDecimal("5000.00"));
        when(stockRepository.countProductsWithStock(TENANT_ID)).thenReturn(10L);

        // When
        StockValuationDto result = stockService.getStockValuation();

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("5000.00"), result.getTotalValue());
        assertEquals(10L, result.getTotalProducts());
    }

    @Test
    void getStockValuation_returnsZero_whenNoStock() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getTotalStockValue(TENANT_ID)).thenReturn(null);
        when(stockRepository.countProductsWithStock(TENANT_ID)).thenReturn(0L);

        // When
        StockValuationDto result = stockService.getStockValuation();

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalValue());
    }

    // ==================== receiveStock ====================

    @Test
    void receiveStock_increasesOnHandQty() {
        // Given
        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(1L)
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .unitCost(new BigDecimal("10.00"))
                        .build()))
                .build();

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("10"))
                .build();

        StockMovementDto movementDto = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("10"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(movement);
        when(stockMovementMapper.toDto(movement)).thenReturn(movementDto);

        // When
        List<StockMovementDto> result = stockService.receiveStock(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(MovementType.STOCK_IN, result.get(0).getMovementType());
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void receiveStock_locationNotFound_throwsNotFoundException() {
        // Given
        StockReceiveRequest request = StockReceiveRequest.builder()
                .locationId(999L)
                .items(List.of(StockReceiveRequest.ReceiveItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> stockService.receiveStock(request));
    }

    // ==================== issueStock ====================

    @Test
    void issueStock_decreasesOnHandQty() {
        // Given
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(1L)
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("7"))
                        .build()))
                .build();

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .movementType(MovementType.STOCK_OUT)
                .quantity(new BigDecimal("7"))
                .build();

        StockMovementDto movementDto = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.STOCK_OUT)
                .quantity(new BigDecimal("7"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(movement);
        when(stockMovementMapper.toDto(movement)).thenReturn(movementDto);

        // When
        List<StockMovementDto> result = stockService.issueStock(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void issueStock_insufficientStock_throwsBusinessException() {
        // Given
        stock.setQuantityOnHand(new BigDecimal("4"));
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(1L)
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));

        // When/Then
        assertThrows(BusinessException.class, () -> stockService.issueStock(request));
    }

    @Test
    void issueStock_productNotFound_throwsNotFoundException() {
        // Given
        StockIssueRequest request = StockIssueRequest.builder()
                .locationId(1L)
                .items(List.of(StockIssueRequest.IssueItem.builder()
                        .productId(999L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> stockService.issueStock(request));
    }

    // ==================== transferStock ====================

    @Test
    void transferStock_decreasesSource_andIncreasesDestination() {
        // Given
        Stock destStock = Stock.builder()
                .id(2L)
                .product(product)
                .location(location2)
                .quantityOnHand(new BigDecimal("10"))
                .quantityReserved(BigDecimal.ZERO)
                .averageCost(new BigDecimal("10.00"))
                .tenantId(TENANT_ID)
                .build();

        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(1L)
                .toLocationId(2L)
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("8"))
                        .build()))
                .build();

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .movementType(MovementType.TRANSFER)
                .quantity(new BigDecimal("8"))
                .build();

        StockMovementDto movementDto = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.TRANSFER)
                .quantity(new BigDecimal("8"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(location2));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 2L, TENANT_ID))
                .thenReturn(Optional.of(destStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(movement);
        when(stockMovementMapper.toDto(movement)).thenReturn(movementDto);

        // When
        List<StockMovementDto> result = stockService.transferStock(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stockRepository, times(2)).save(any(Stock.class));
    }

    @Test
    void transferStock_sameLocation_throwsBusinessException() {
        // Given
        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(1L)
                .toLocationId(1L)
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("5"))
                        .build()))
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> stockService.transferStock(request));
    }

    @Test
    void transferStock_insufficientStock_throwsBusinessException() {
        // Given
        stock.setQuantityOnHand(new BigDecimal("2"));
        StockTransferRequest request = StockTransferRequest.builder()
                .fromLocationId(1L)
                .toLocationId(2L)
                .items(List.of(StockTransferRequest.TransferItem.builder()
                        .productId(1L)
                        .quantity(new BigDecimal("10"))
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(location2));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));

        // When/Then
        assertThrows(BusinessException.class, () -> stockService.transferStock(request));
    }

    // ==================== adjustStock ====================

    @Test
    void adjustStock_positiveAdjustment_increasesQuantity() {
        // Given
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(1L)
                .reason("Count adjustment")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(1L)
                        .adjustmentQuantity(new BigDecimal("5"))
                        .build()))
                .build();

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(new BigDecimal("5"))
                .build();

        StockMovementDto movementDto = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(new BigDecimal("5"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(movement);
        when(stockMovementMapper.toDto(movement)).thenReturn(movementDto);

        // When
        List<StockMovementDto> result = stockService.adjustStock(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void adjustStock_negativeAdjustment_decreasesQuantity() {
        // Given
        StockAdjustRequest request = StockAdjustRequest.builder()
                .locationId(1L)
                .reason("Damaged goods")
                .items(List.of(StockAdjustRequest.AdjustItem.builder()
                        .productId(1L)
                        .adjustmentQuantity(new BigDecimal("-3"))
                        .build()))
                .build();

        StockMovement movement = StockMovement.builder()
                .id(1L)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(new BigDecimal("3"))
                .build();

        StockMovementDto movementDto = StockMovementDto.builder()
                .id(1L)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(new BigDecimal("3"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdWithLock(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(movement);
        when(stockMovementMapper.toDto(movement)).thenReturn(movementDto);

        // When
        List<StockMovementDto> result = stockService.adjustStock(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== getStockMovements ====================

    @Test
    void getStockMovements_returnsPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        StockMovement movement = StockMovement.builder().id(1L).movementType(MovementType.STOCK_IN).build();
        Page<StockMovement> page = new PageImpl<>(List.of(movement), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockMovementRepository.findByTenantIdOrderByMovementDateDesc(TENANT_ID, pageable)).thenReturn(page);
        when(stockMovementMapper.toDto(movement)).thenReturn(StockMovementDto.builder().id(1L).build());

        // When
        PageResponse<StockMovementDto> result = stockService.getStockMovements(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== isStockAvailable ====================

    @Test
    void isStockAvailable_enoughStock_returnsTrue() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getAvailableQuantityByProduct(1L, TENANT_ID)).thenReturn(new BigDecimal("50"));

        // When
        boolean result = stockService.isStockAvailable(1L, 1L, new BigDecimal("10"));

        // Then
        assertTrue(result);
    }

    @Test
    void isStockAvailable_insufficientStock_returnsFalse() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getAvailableQuantityByProduct(1L, TENANT_ID)).thenReturn(new BigDecimal("3"));

        // When
        boolean result = stockService.isStockAvailable(1L, 1L, new BigDecimal("10"));

        // Then
        assertFalse(result);
    }

    @Test
    void isStockAvailable_noStockRecord_returnsFalse() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getAvailableQuantityByProduct(1L, TENANT_ID)).thenReturn(null);

        // When
        boolean result = stockService.isStockAvailable(1L, 1L, new BigDecimal("5"));

        // Then
        assertFalse(result);
    }

    // ==================== getAvailableQuantity ====================

    @Test
    void getAvailableQuantity_returnsQuantity() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getAvailableQuantityByProduct(1L, TENANT_ID)).thenReturn(new BigDecimal("50"));

        // When
        BigDecimal result = stockService.getAvailableQuantity(1L);

        // Then
        assertEquals(new BigDecimal("50"), result);
    }

    @Test
    void getAvailableQuantity_noStock_returnsZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getAvailableQuantityByProduct(1L, TENANT_ID)).thenReturn(null);

        // When
        BigDecimal result = stockService.getAvailableQuantity(1L);

        // Then
        assertEquals(BigDecimal.ZERO, result);
    }
}
