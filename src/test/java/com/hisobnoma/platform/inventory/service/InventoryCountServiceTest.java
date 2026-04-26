package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.InventoryCountMapper;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceTest {

    @Mock
    private InventoryCountRepository inventoryCountRepository;
    @Mock
    private InventoryCountLineRepository inventoryCountLineRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private UnitOfMeasureRepository unitOfMeasureRepository;
    @Mock
    private InventoryCountMapper inventoryCountMapper;
    @Mock
    private StockService stockService;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private InventoryCountService inventoryCountService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private Location location;
    private Category category;
    private Product product;
    private UnitOfMeasure uom;
    private InventoryCount inventoryCount;
    private InventoryCountDto inventoryCountDto;
    private InventoryCountLine countLine;
    private InventoryCountLineDto countLineDto;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
                .tenantId(TENANT_ID)
                .build();

        category = Category.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .tenantId(TENANT_ID)
                .build();

        uom = UnitOfMeasure.builder()
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
                .tenantId(TENANT_ID)
                .build();

        inventoryCount = InventoryCount.builder()
                .id(1L)
                .countNumber("CNT-202604-0001")
                .location(location)
                .status(CountStatus.DRAFT)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.now())
                .description("Test count")
                .tenantId(TENANT_ID)
                .lines(new ArrayList<>())
                .build();

        inventoryCountDto = InventoryCountDto.builder()
                .id(1L)
                .countNumber("CNT-202604-0001")
                .locationId(1L)
                .locationName("Main Warehouse")
                .status(CountStatus.DRAFT)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.now())
                .build();

        countLine = InventoryCountLine.builder()
                .id(1L)
                .inventoryCount(inventoryCount)
                .product(product)
                .lineNumber(1)
                .systemQuantity(new BigDecimal("50"))
                .unitCost(new BigDecimal("100.00"))
                .uom(uom)
                .counted(false)
                .tenantId(TENANT_ID)
                .build();

        countLineDto = InventoryCountLineDto.builder()
                .id(1L)
                .inventoryCountId(1L)
                .productId(1L)
                .productSku("PROD-001")
                .productName("Test Product")
                .lineNumber(1)
                .systemQuantity(new BigDecimal("50"))
                .unitCost(new BigDecimal("100.00"))
                .counted(false)
                .build();
    }

    // ==================== getInventoryCounts ====================

    @Test
    void getInventoryCounts_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryCount> page = new PageImpl<>(List.of(inventoryCount), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        PageResponse<InventoryCountDto> result = inventoryCountService.getInventoryCounts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(inventoryCountRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    @Test
    void getInventoryCounts_returnsEmpty_whenNone() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryCount> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<InventoryCountDto> result = inventoryCountService.getInventoryCounts(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getInventoryCount ====================

    @Test
    void getInventoryCount_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountMapper.toDto(inventoryCount)).thenReturn(inventoryCountDto);
        when(inventoryCountMapper.toLineDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        InventoryCountDto result = inventoryCountService.getInventoryCount(1L);

        // Then
        assertNotNull(result);
        assertEquals("CNT-202604-0001", result.getCountNumber());
    }

    @Test
    void getInventoryCount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.getInventoryCount(999L));
    }

    // ==================== getInventoryCountsByStatus ====================

    @Test
    void getInventoryCountsByStatus_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryCount> page = new PageImpl<>(List.of(inventoryCount), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByStatusAndTenantId(CountStatus.DRAFT, TENANT_ID, pageable))
                .thenReturn(page);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        PageResponse<InventoryCountDto> result = inventoryCountService.getInventoryCountsByStatus(CountStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== searchInventoryCounts ====================

    @Test
    void searchInventoryCounts_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryCount> page = new PageImpl<>(List.of(inventoryCount), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.searchByTenantId(TENANT_ID, "CNT", pageable)).thenReturn(page);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        PageResponse<InventoryCountDto> result = inventoryCountService.searchInventoryCounts("CNT", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void searchInventoryCounts_noMatch_returnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryCount> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.searchByTenantId(TENANT_ID, "NONEXISTENT", pageable)).thenReturn(emptyPage);

        // When
        PageResponse<InventoryCountDto> result = inventoryCountService.searchInventoryCounts("NONEXISTENT", pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== createInventoryCount ====================

    @Test
    void createInventoryCount_success_withProductIds() {
        // Given
        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(1L)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.now())
                .description("Test count")
                .productIds(List.of(1L))
                .build();

        Stock stock = Stock.builder()
                .id(1L)
                .product(product)
                .location(location)
                .quantityOnHand(new BigDecimal("50"))
                .averageCost(new BigDecimal("100.00"))
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(inventoryCountRepository.findActiveCountsForLocation(1L, TENANT_ID))
                .thenReturn(Collections.emptyList());
        when(inventoryCountRepository.findMaxCountNumberByTenantId(TENANT_ID)).thenReturn(null);
        when(inventoryCountRepository.save(any(InventoryCount.class))).thenReturn(inventoryCount);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndLocationIdAndTenantId(1L, 1L, TENANT_ID))
                .thenReturn(Optional.of(stock));
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);
        when(inventoryCountMapper.toLineDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        InventoryCountDto result = inventoryCountService.createInventoryCount(request);

        // Then
        assertNotNull(result);
        verify(inventoryCountRepository, atLeastOnce()).save(any(InventoryCount.class));
    }

    @Test
    void createInventoryCount_locationNotFound_throwsNotFoundException() {
        // Given
        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(999L)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.createInventoryCount(request));
        verify(inventoryCountRepository, never()).save(any());
    }

    @Test
    void createInventoryCount_activeCountExists_throwsBusinessException() {
        // Given
        CreateInventoryCountRequest request = CreateInventoryCountRequest.builder()
                .locationId(1L)
                .countType(CountType.PARTIAL)
                .countDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(inventoryCountRepository.findActiveCountsForLocation(1L, TENANT_ID))
                .thenReturn(List.of(inventoryCount));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.createInventoryCount(request));
        verify(inventoryCountRepository, never()).save(any());
    }

    // ==================== startCount ====================

    @Test
    void startCount_success_setsStatusToInProgress() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountRepository.save(any(InventoryCount.class))).thenReturn(inventoryCount);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        InventoryCountDto result = inventoryCountService.startCount(1L);

        // Then
        assertNotNull(result);
        assertEquals(CountStatus.IN_PROGRESS, inventoryCount.getStatus());
        assertNotNull(inventoryCount.getStartedAt());
        verify(inventoryCountRepository).save(inventoryCount);
    }

    @Test
    void startCount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.startCount(999L));
    }

    @Test
    void startCount_notDraftStatus_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.startCount(1L));
        verify(inventoryCountRepository, never()).save(any());
    }

    // ==================== recordCount ====================

    @Test
    void recordCount_success_recordsQuantity() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("48"))
                .notes("Two items damaged")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountLineRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(countLine));
        when(inventoryCountLineRepository.save(any(InventoryCountLine.class))).thenReturn(countLine);
        when(inventoryCountRepository.save(any(InventoryCount.class))).thenReturn(inventoryCount);
        when(inventoryCountMapper.toLineDto(any(InventoryCountLine.class))).thenReturn(countLineDto);

        // When
        InventoryCountLineDto result = inventoryCountService.recordCount(1L, 1L, request);

        // Then
        assertNotNull(result);
        verify(inventoryCountLineRepository).save(any(InventoryCountLine.class));
        verify(inventoryCountRepository).save(inventoryCount);
    }

    @Test
    void recordCount_countNotFound_throwsNotFoundException() {
        // Given
        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("48"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.recordCount(999L, 1L, request));
    }

    @Test
    void recordCount_notInProgress_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.DRAFT);

        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("48"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.recordCount(1L, 1L, request));
    }

    @Test
    void recordCount_lineNotFound_throwsNotFoundException() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("48"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountLineRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.recordCount(1L, 999L, request));
    }

    @Test
    void recordCount_lineBelongsToDifferentCount_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        InventoryCount otherCount = InventoryCount.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .build();
        countLine.setInventoryCount(otherCount);

        RecordCountRequest request = RecordCountRequest.builder()
                .countedQuantity(new BigDecimal("48"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountLineRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(countLine));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.recordCount(1L, 1L, request));
    }

    // ==================== completeCount ====================

    @Test
    void completeCount_success_setsStatusToCompleted() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountLineRepository.findUncountedLinesByCountId(1L, TENANT_ID))
                .thenReturn(Collections.emptyList());
        when(inventoryCountRepository.save(any(InventoryCount.class))).thenReturn(inventoryCount);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        InventoryCountDto result = inventoryCountService.completeCount(1L);

        // Then
        assertNotNull(result);
        assertEquals(CountStatus.COMPLETED, inventoryCount.getStatus());
        assertNotNull(inventoryCount.getCompletedAt());
    }

    @Test
    void completeCount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.completeCount(999L));
    }

    @Test
    void completeCount_notInProgress_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.DRAFT);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.completeCount(1L));
        verify(inventoryCountRepository, never()).save(any());
    }

    @Test
    void completeCount_uncountedItems_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountLineRepository.findUncountedLinesByCountId(1L, TENANT_ID))
                .thenReturn(List.of(countLine));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.completeCount(1L));
        verify(inventoryCountRepository, never()).save(any());
    }

    // ==================== approveCount ====================

    @Test
    void approveCount_success_postsAdjustments() {
        // Given
        inventoryCount.setStatus(CountStatus.COMPLETED);
        countLine.setCountedQuantity(new BigDecimal("48"));
        countLine.setCounted(true);
        countLine.setVarianceQuantity(new BigDecimal("-2"));
        countLine.setVarianceValue(new BigDecimal("-200.00"));
        inventoryCount.setLines(new ArrayList<>(List.of(countLine)));

        StockMovementDto movementDto = StockMovementDto.builder().id(100L).build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(stockService.adjustStock(any(StockAdjustRequest.class)))
                .thenReturn(List.of(movementDto));
        when(inventoryCountRepository.save(any(InventoryCount.class))).thenReturn(inventoryCount);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        InventoryCountDto result = inventoryCountService.approveCount(1L);

        // Then
        assertNotNull(result);
        assertEquals(CountStatus.APPROVED, inventoryCount.getStatus());
        assertTrue(inventoryCount.isAdjustmentsPosted());
        verify(stockService).adjustStock(any(StockAdjustRequest.class));
    }

    @Test
    void approveCount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.approveCount(999L));
    }

    @Test
    void approveCount_notCompleted_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.IN_PROGRESS);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.approveCount(1L));
        verify(stockService, never()).adjustStock(any());
    }

    // ==================== cancelCount ====================

    @Test
    void cancelCount_success_setsStatusToCancelled() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));
        when(inventoryCountRepository.save(any(InventoryCount.class))).thenReturn(inventoryCount);
        when(inventoryCountMapper.toDto(any(InventoryCount.class))).thenReturn(inventoryCountDto);

        // When
        InventoryCountDto result = inventoryCountService.cancelCount(1L, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(CountStatus.CANCELLED, inventoryCount.getStatus());
        assertEquals("No longer needed", inventoryCount.getCancellationReason());
        verify(inventoryCountRepository).save(inventoryCount);
    }

    @Test
    void cancelCount_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> inventoryCountService.cancelCount(999L, "reason"));
    }

    @Test
    void cancelCount_approvedStatus_throwsBusinessException() {
        // Given
        inventoryCount.setStatus(CountStatus.APPROVED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(inventoryCountRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(inventoryCount));

        // When/Then
        assertThrows(BusinessException.class, () -> inventoryCountService.cancelCount(1L, "reason"));
        verify(inventoryCountRepository, never()).save(any());
    }

    // ==================== markForRecount ====================

    @Test
    void markForRecount_success_setsRecountRequired() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountLineRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(countLine));

        // When
        inventoryCountService.markForRecount(1L, 1L, "Suspicious count");

        // Then
        assertTrue(countLine.isRecountRequired());
        assertEquals("Suspicious count", countLine.getNotes());
        verify(inventoryCountLineRepository).save(countLine);
    }

    @Test
    void markForRecount_lineNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountLineRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> inventoryCountService.markForRecount(1L, 999L, "reason"));
    }

    @Test
    void markForRecount_lineBelongsToDifferentCount_throwsBusinessException() {
        // Given
        InventoryCount otherCount = InventoryCount.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .build();
        countLine.setInventoryCount(otherCount);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(inventoryCountLineRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(countLine));

        // When/Then
        assertThrows(BusinessException.class,
                () -> inventoryCountService.markForRecount(1L, 1L, "reason"));
        verify(inventoryCountLineRepository, never()).save(any());
    }
}
