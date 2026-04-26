package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.service.APInvoiceService;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.ReceivingOrderMapper;
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
class ReceivingServiceTest {

    @Mock
    private ReceivingOrderRepository receivingOrderRepository;
    @Mock
    private ReceivingLineRepository receivingLineRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UnitOfMeasureRepository unitOfMeasureRepository;
    @Mock
    private ReceivingOrderMapper receivingOrderMapper;
    @Mock
    private StockService stockService;
    @Mock
    private APInvoiceService apInvoiceService;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ReceivingService receivingService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private Vendor vendor;
    private Location location;
    private Product product;
    private UnitOfMeasure uom;
    private PurchaseOrder purchaseOrder;
    private ReceivingOrder receivingOrder;
    private ReceivingOrderDto receivingOrderDto;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .id(1L)
                .code("VND-001")
                .name("Test Vendor")
                .tenantId(TENANT_ID)
                .build();

        location = Location.builder()
                .id(1L)
                .code("LOC-001")
                .name("Main Warehouse")
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
                .baseUom(uom)
                .costPrice(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("150.00"))
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        purchaseOrder = PurchaseOrder.builder()
                .id(1L)
                .poNumber("PO-001")
                .vendor(vendor)
                .location(location)
                .status(POStatus.APPROVED)
                .orderDate(LocalDate.now())
                .tenantId(TENANT_ID)
                .build();

        receivingOrder = ReceivingOrder.builder()
                .id(1L)
                .receivingNumber("RCV-202604-0001")
                .vendor(vendor)
                .location(location)
                .status(ReceivingStatus.DRAFT)
                .receivingDate(LocalDate.now())
                .tenantId(TENANT_ID)
                .lines(new ArrayList<>())
                .build();

        receivingOrderDto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-202604-0001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .locationId(1L)
                .locationName("Main Warehouse")
                .status(ReceivingStatus.DRAFT)
                .receivingDate(LocalDate.now())
                .build();
    }

    // ==================== getReceivingOrders ====================

    @Test
    void getReceivingOrders_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReceivingOrder> page = new PageImpl<>(List.of(receivingOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);

        // When
        PageResponse<ReceivingOrderDto> result = receivingService.getReceivingOrders(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(receivingOrderRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    @Test
    void getReceivingOrders_returnsEmpty_whenNone() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReceivingOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<ReceivingOrderDto> result = receivingService.getReceivingOrders(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getReceivingOrder ====================

    @Test
    void getReceivingOrder_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));
        when(receivingOrderMapper.toDto(receivingOrder)).thenReturn(receivingOrderDto);
        when(receivingOrderMapper.toLineDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        ReceivingOrderDto result = receivingService.getReceivingOrder(1L);

        // Then
        assertNotNull(result);
        assertEquals("RCV-202604-0001", result.getReceivingNumber());
    }

    @Test
    void getReceivingOrder_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> receivingService.getReceivingOrder(999L));
    }

    // ==================== getReceivingOrdersByStatus ====================

    @Test
    void getReceivingOrdersByStatus_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReceivingOrder> page = new PageImpl<>(List.of(receivingOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByStatusAndTenantId(ReceivingStatus.DRAFT, TENANT_ID, pageable))
                .thenReturn(page);
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);

        // When
        PageResponse<ReceivingOrderDto> result = receivingService.getReceivingOrdersByStatus(ReceivingStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== getReceivingOrdersForPO ====================

    @Test
    void getReceivingOrdersForPO_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.findByPurchaseOrderIdAndTenantId(1L, TENANT_ID))
                .thenReturn(List.of(receivingOrder));
        when(receivingOrderMapper.toDtoList(anyList())).thenReturn(List.of(receivingOrderDto));

        // When
        List<ReceivingOrderDto> result = receivingService.getReceivingOrdersForPO(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== searchReceivingOrders ====================

    @Test
    void searchReceivingOrders_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReceivingOrder> page = new PageImpl<>(List.of(receivingOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.searchByTenantId(TENANT_ID, "RCV", pageable)).thenReturn(page);
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);

        // When
        PageResponse<ReceivingOrderDto> result = receivingService.searchReceivingOrders("RCV", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void searchReceivingOrders_noMatch_returnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReceivingOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(receivingOrderRepository.searchByTenantId(TENANT_ID, "NONEXISTENT", pageable)).thenReturn(emptyPage);

        // When
        PageResponse<ReceivingOrderDto> result = receivingService.searchReceivingOrders("NONEXISTENT", pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== createReceivingOrder ====================

    @Test
    void createReceivingOrder_success_withoutPO() {
        // Given
        CreateReceivingLineRequest lineRequest = CreateReceivingLineRequest.builder()
                .productId(1L)
                .uomId(1L)
                .receivedQuantity(new BigDecimal("10"))
                .unitCost(new BigDecimal("100.00"))
                .build();

        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .receivingDate(LocalDate.now())
                .lines(List.of(lineRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(unitOfMeasureRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(uom));
        when(receivingOrderRepository.findMaxReceivingNumberByTenantId(TENANT_ID)).thenReturn(null);
        when(receivingOrderRepository.save(any(ReceivingOrder.class))).thenReturn(receivingOrder);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);
        when(receivingOrderMapper.toLineDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        ReceivingOrderDto result = receivingService.createReceivingOrder(request);

        // Then
        assertNotNull(result);
        verify(receivingOrderRepository).save(any(ReceivingOrder.class));
    }

    @Test
    void createReceivingOrder_success_withApprovedPO() {
        // Given
        CreateReceivingLineRequest lineRequest = CreateReceivingLineRequest.builder()
                .productId(1L)
                .uomId(1L)
                .receivedQuantity(new BigDecimal("10"))
                .unitCost(new BigDecimal("100.00"))
                .build();

        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .purchaseOrderId(1L)
                .receivingDate(LocalDate.now())
                .lines(List.of(lineRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(unitOfMeasureRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(uom));
        when(receivingOrderRepository.findMaxReceivingNumberByTenantId(TENANT_ID)).thenReturn(null);
        when(receivingOrderRepository.save(any(ReceivingOrder.class))).thenReturn(receivingOrder);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);
        when(receivingOrderMapper.toLineDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        ReceivingOrderDto result = receivingService.createReceivingOrder(request);

        // Then
        assertNotNull(result);
        verify(purchaseOrderRepository).findByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void createReceivingOrder_vendorNotFound_throwsNotFoundException() {
        // Given
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(999L)
                .locationId(1L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .uomId(1L)
                        .receivedQuantity(BigDecimal.TEN)
                        .unitCost(BigDecimal.TEN)
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> receivingService.createReceivingOrder(request));
        verify(receivingOrderRepository, never()).save(any());
    }

    @Test
    void createReceivingOrder_locationNotFound_throwsNotFoundException() {
        // Given
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(999L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .uomId(1L)
                        .receivedQuantity(BigDecimal.TEN)
                        .unitCost(BigDecimal.TEN)
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> receivingService.createReceivingOrder(request));
        verify(receivingOrderRepository, never()).save(any());
    }

    @Test
    void createReceivingOrder_poNotFound_throwsNotFoundException() {
        // Given
        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .purchaseOrderId(999L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .uomId(1L)
                        .receivedQuantity(BigDecimal.TEN)
                        .unitCost(BigDecimal.TEN)
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(purchaseOrderRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> receivingService.createReceivingOrder(request));
        verify(receivingOrderRepository, never()).save(any());
    }

    @Test
    void createReceivingOrder_poInvalidStatus_throwsBusinessException() {
        // Given
        PurchaseOrder draftPo = PurchaseOrder.builder()
                .id(2L)
                .poNumber("PO-002")
                .status(POStatus.DRAFT)
                .tenantId(TENANT_ID)
                .build();

        CreateReceivingRequest request = CreateReceivingRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .purchaseOrderId(2L)
                .receivingDate(LocalDate.now())
                .lines(List.of(CreateReceivingLineRequest.builder()
                        .productId(1L)
                        .uomId(1L)
                        .receivedQuantity(BigDecimal.TEN)
                        .unitCost(BigDecimal.TEN)
                        .build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(purchaseOrderRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(draftPo));

        // When/Then
        assertThrows(BusinessException.class, () -> receivingService.createReceivingOrder(request));
        verify(receivingOrderRepository, never()).save(any());
    }

    // ==================== confirmReceiving ====================

    @Test
    void confirmReceiving_success_updatesStockAndStatus() {
        // Given
        ReceivingLine line = ReceivingLine.builder()
                .id(1L)
                .product(product)
                .acceptedQuantity(new BigDecimal("10"))
                .unitCost(new BigDecimal("100.00"))
                .receivedQuantity(new BigDecimal("10"))
                .tenantId(TENANT_ID)
                .build();

        receivingOrder.setLines(new ArrayList<>(List.of(line)));
        line.setReceivingOrder(receivingOrder);

        StockMovementDto movementDto = StockMovementDto.builder().id(100L).build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));
        when(stockService.receiveStock(any(StockReceiveRequest.class)))
                .thenReturn(List.of(movementDto));
        when(receivingOrderRepository.save(any(ReceivingOrder.class))).thenReturn(receivingOrder);
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);

        // When
        ReceivingOrderDto result = receivingService.confirmReceiving(1L);

        // Then
        assertNotNull(result);
        assertEquals(ReceivingStatus.COMPLETED, receivingOrder.getStatus());
        assertTrue(receivingOrder.isStockUpdated());
        verify(stockService).receiveStock(any(StockReceiveRequest.class));
        verify(receivingOrderRepository).save(receivingOrder);
    }

    @Test
    void confirmReceiving_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> receivingService.confirmReceiving(999L));
    }

    @Test
    void confirmReceiving_completedStatus_throwsBusinessException() {
        // Given
        receivingOrder.setStatus(ReceivingStatus.COMPLETED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> receivingService.confirmReceiving(1L));
        verify(stockService, never()).receiveStock(any());
    }

    @Test
    void confirmReceiving_cancelledStatus_throwsBusinessException() {
        // Given
        receivingOrder.setStatus(ReceivingStatus.CANCELLED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> receivingService.confirmReceiving(1L));
    }

    @Test
    void confirmReceiving_withPurchaseOrder_updatesPOStatus() {
        // Given
        PurchaseOrderLine poLine = PurchaseOrderLine.builder()
                .id(1L)
                .quantity(new BigDecimal("10"))
                .receivedQuantity(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        ReceivingLine line = ReceivingLine.builder()
                .id(1L)
                .product(product)
                .acceptedQuantity(new BigDecimal("10"))
                .unitCost(new BigDecimal("100.00"))
                .receivedQuantity(new BigDecimal("10"))
                .poLine(poLine)
                .tenantId(TENANT_ID)
                .build();

        receivingOrder.setPurchaseOrder(purchaseOrder);
        receivingOrder.setLines(new ArrayList<>(List.of(line)));
        line.setReceivingOrder(receivingOrder);

        StockMovementDto movementDto = StockMovementDto.builder().id(100L).build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));
        when(stockService.receiveStock(any(StockReceiveRequest.class)))
                .thenReturn(List.of(movementDto));
        when(receivingOrderRepository.save(any(ReceivingOrder.class))).thenReturn(receivingOrder);
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        when(purchaseOrderLineRepository.save(any(PurchaseOrderLine.class))).thenReturn(poLine);

        // When
        ReceivingOrderDto result = receivingService.confirmReceiving(1L);

        // Then
        assertNotNull(result);
        verify(purchaseOrderLineRepository).save(any(PurchaseOrderLine.class));
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    // ==================== cancelReceiving ====================

    @Test
    void cancelReceiving_success_setsStatusToCancelled() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));
        when(receivingOrderRepository.save(any(ReceivingOrder.class))).thenReturn(receivingOrder);
        when(receivingOrderMapper.toDto(any(ReceivingOrder.class))).thenReturn(receivingOrderDto);

        // When
        ReceivingOrderDto result = receivingService.cancelReceiving(1L, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(ReceivingStatus.CANCELLED, receivingOrder.getStatus());
        assertEquals("No longer needed", receivingOrder.getCancellationReason());
        verify(receivingOrderRepository).save(receivingOrder);
    }

    @Test
    void cancelReceiving_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdAndTenantId(999L, TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> receivingService.cancelReceiving(999L, "reason"));
    }

    @Test
    void cancelReceiving_completedStatus_throwsBusinessException() {
        // Given
        receivingOrder.setStatus(ReceivingStatus.COMPLETED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(receivingOrderRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(receivingOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> receivingService.cancelReceiving(1L, "reason"));
        verify(receivingOrderRepository, never()).save(any());
    }
}
