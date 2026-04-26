package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.PurchaseOrderMapper;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

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
    private PurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private ReceivingService receivingService;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private PurchaseOrder purchaseOrder;
    private PurchaseOrderDto purchaseOrderDto;
    private Vendor vendor;
    private Location location;
    private Product product;
    private UnitOfMeasure uom;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .id(1L)
                .code("VND-001")
                .name("Acme Supplies")
                .tenantId(TENANT_ID)
                .build();

        location = Location.builder()
                .id(1L)
                .code("WH-01")
                .name("Main Warehouse")
                .tenantId(TENANT_ID)
                .build();

        product = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .tenantId(TENANT_ID)
                .build();

        uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .tenantId(TENANT_ID)
                .build();

        PurchaseOrderLine line = PurchaseOrderLine.builder()
                .id(1L)
                .product(product)
                .lineNumber(1)
                .quantity(BigDecimal.TEN)
                .receivedQuantity(BigDecimal.ZERO)
                .cancelledQuantity(BigDecimal.ZERO)
                .uom(uom)
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(new BigDecimal("1000.00"))
                .tenantId(TENANT_ID)
                .build();

        purchaseOrder = PurchaseOrder.builder()
                .id(1L)
                .poNumber("PO-202604-0001")
                .vendor(vendor)
                .location(location)
                .status(POStatus.DRAFT)
                .orderDate(LocalDate.now())
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1000.00"))
                .currency("UZS")
                .lines(new ArrayList<>(List.of(line)))
                .tenantId(TENANT_ID)
                .build();
        line.setPurchaseOrder(purchaseOrder);

        purchaseOrderDto = PurchaseOrderDto.builder()
                .id(1L)
                .poNumber("PO-202604-0001")
                .vendorId(1L)
                .vendorName("Acme Supplies")
                .locationId(1L)
                .locationName("Main Warehouse")
                .status(POStatus.DRAFT)
                .orderDate(LocalDate.now())
                .totalAmount(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void getPurchaseOrders_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseOrder> page = new PageImpl<>(List.of(purchaseOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(purchaseOrderDto);

        // When
        PageResponse<PurchaseOrderDto> result = purchaseOrderService.getPurchaseOrders(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPurchaseOrder_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(purchaseOrderDto);
        when(purchaseOrderMapper.toLineDtoList(anyList())).thenReturn(List.of());

        // When
        PurchaseOrderDto result = purchaseOrderService.getPurchaseOrder(1L);

        // Then
        assertNotNull(result);
        assertEquals("PO-202604-0001", result.getPoNumber());
    }

    @Test
    void getPurchaseOrder_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> purchaseOrderService.getPurchaseOrder(999L));
    }

    @Test
    void getPurchaseOrdersByStatus_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseOrder> page = new PageImpl<>(List.of(purchaseOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByStatusAndTenantId(POStatus.DRAFT, TENANT_ID, pageable)).thenReturn(page);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(purchaseOrderDto);

        // When
        PageResponse<PurchaseOrderDto> result = purchaseOrderService.getPurchaseOrdersByStatus(POStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPurchaseOrdersByVendor_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseOrder> page = new PageImpl<>(List.of(purchaseOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByVendorIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(purchaseOrderDto);

        // When
        PageResponse<PurchaseOrderDto> result = purchaseOrderService.getPurchaseOrdersByVendor(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void searchPurchaseOrders_returnsMatches() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseOrder> page = new PageImpl<>(List.of(purchaseOrder), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.searchByTenantId(TENANT_ID, "PO-202604", pageable)).thenReturn(page);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(purchaseOrderDto);

        // When
        PageResponse<PurchaseOrderDto> result = purchaseOrderService.searchPurchaseOrders("PO-202604", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createPurchaseOrder_success() {
        // Given
        CreatePurchaseOrderLineRequest lineRequest = CreatePurchaseOrderLineRequest.builder()
                .productId(1L)
                .quantity(BigDecimal.TEN)
                .uomId(1L)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(1L)
                .locationId(1L)
                .orderDate(LocalDate.now())
                .lines(List.of(lineRequest))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(unitOfMeasureRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(uom));
        when(purchaseOrderRepository.findMaxPoNumberByTenantId(TENANT_ID)).thenReturn(null);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(anyLong(), eq(TENANT_ID)))
                .thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderMapper.toDto(any(PurchaseOrder.class))).thenReturn(purchaseOrderDto);
        when(purchaseOrderMapper.toLineDtoList(anyList())).thenReturn(List.of());

        // When
        PurchaseOrderDto result = purchaseOrderService.createPurchaseOrder(request);

        // Then
        assertNotNull(result);
        verify(purchaseOrderRepository, atLeastOnce()).save(any(PurchaseOrder.class));
    }

    @Test
    void createPurchaseOrder_vendorNotFound_throwsNotFoundException() {
        // Given
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(999L)
                .locationId(1L)
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(1L).quantity(BigDecimal.TEN).uomId(1L).unitPrice(BigDecimal.TEN).build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> purchaseOrderService.createPurchaseOrder(request));
    }

    @Test
    void createPurchaseOrder_locationNotFound_throwsNotFoundException() {
        // Given
        CreatePurchaseOrderRequest request = CreatePurchaseOrderRequest.builder()
                .vendorId(1L)
                .locationId(999L)
                .orderDate(LocalDate.now())
                .lines(List.of(CreatePurchaseOrderLineRequest.builder()
                        .productId(1L).quantity(BigDecimal.TEN).uomId(1L).unitPrice(BigDecimal.TEN).build()))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> purchaseOrderService.createPurchaseOrder(request));
    }

    @Test
    void approvePurchaseOrder_draftStatus_success() {
        // Given
        purchaseOrder.setStatus(POStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(
                PurchaseOrderDto.builder().id(1L).status(POStatus.APPROVED).build());

        // When
        PurchaseOrderDto result = purchaseOrderService.approvePurchaseOrder(1L);

        // Then
        assertNotNull(result);
        assertEquals(POStatus.APPROVED, purchaseOrder.getStatus());
        assertEquals(USER_ID, purchaseOrder.getApprovedBy());
        assertNotNull(purchaseOrder.getApprovedAt());
    }

    @Test
    void approvePurchaseOrder_pendingStatus_success() {
        // Given
        purchaseOrder.setStatus(POStatus.PENDING);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(
                PurchaseOrderDto.builder().id(1L).status(POStatus.APPROVED).build());

        // When
        PurchaseOrderDto result = purchaseOrderService.approvePurchaseOrder(1L);

        // Then
        assertNotNull(result);
        assertEquals(POStatus.APPROVED, purchaseOrder.getStatus());
    }

    @Test
    void approvePurchaseOrder_receivedStatus_throwsBusinessException() {
        // Given
        purchaseOrder.setStatus(POStatus.RECEIVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> purchaseOrderService.approvePurchaseOrder(1L));
    }

    @Test
    void approvePurchaseOrder_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> purchaseOrderService.approvePurchaseOrder(999L));
    }

    @Test
    void cancelPurchaseOrder_draftStatus_success() {
        // Given
        purchaseOrder.setStatus(POStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(
                PurchaseOrderDto.builder().id(1L).status(POStatus.CANCELLED).build());

        // When
        PurchaseOrderDto result = purchaseOrderService.cancelPurchaseOrder(1L, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(POStatus.CANCELLED, purchaseOrder.getStatus());
        assertEquals("No longer needed", purchaseOrder.getCancellationReason());
        assertEquals(USER_ID, purchaseOrder.getCancelledBy());
        assertNotNull(purchaseOrder.getCancelledAt());
    }

    @Test
    void cancelPurchaseOrder_receivedStatus_throwsBusinessException() {
        // Given
        purchaseOrder.setStatus(POStatus.RECEIVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> purchaseOrderService.cancelPurchaseOrder(1L, "Reason"));
    }

    @Test
    void cancelPurchaseOrder_closedStatus_throwsBusinessException() {
        // Given
        purchaseOrder.setStatus(POStatus.CLOSED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> purchaseOrderService.cancelPurchaseOrder(1L, "Reason"));
    }

    @Test
    void submitForApproval_draftStatus_success() {
        // Given
        purchaseOrder.setStatus(POStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(
                PurchaseOrderDto.builder().id(1L).status(POStatus.PENDING).build());

        // When
        PurchaseOrderDto result = purchaseOrderService.submitForApproval(1L);

        // Then
        assertNotNull(result);
        assertEquals(POStatus.PENDING, purchaseOrder.getStatus());
    }

    @Test
    void submitForApproval_notDraft_throwsBusinessException() {
        // Given
        purchaseOrder.setStatus(POStatus.APPROVED);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(purchaseOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> purchaseOrderService.submitForApproval(1L));
    }

    @Test
    void receivePurchaseOrder_approvedStatus_success() {
        // Given
        purchaseOrder.setStatus(POStatus.APPROVED);

        ReceivingOrderDto roDto = ReceivingOrderDto.builder()
                .id(1L)
                .receivingNumber("RCV-202604-0001")
                .status(ReceivingStatus.DRAFT)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(purchaseOrder));
        when(receivingService.createReceivingOrder(any(CreateReceivingRequest.class))).thenReturn(roDto);
        doNothing().when(receivingService).confirmReceiving(anyLong());

        // For the final getPurchaseOrder call
        when(purchaseOrderMapper.toDto(purchaseOrder)).thenReturn(purchaseOrderDto);
        when(purchaseOrderMapper.toLineDtoList(anyList())).thenReturn(List.of());

        // When
        PurchaseOrderDto result = purchaseOrderService.receivePurchaseOrder(1L);

        // Then
        assertNotNull(result);
        verify(receivingService).createReceivingOrder(any(CreateReceivingRequest.class));
        verify(receivingService).confirmReceiving(1L);
    }

    @Test
    void receivePurchaseOrder_draftStatus_throwsBusinessException() {
        // Given
        purchaseOrder.setStatus(POStatus.DRAFT);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(purchaseOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> purchaseOrderService.receivePurchaseOrder(1L));
    }

    @Test
    void receivePurchaseOrder_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> purchaseOrderService.receivePurchaseOrder(999L));
    }

    @Test
    void receivePurchaseOrder_noPendingQuantities_throwsBusinessException() {
        // Given
        purchaseOrder.setStatus(POStatus.APPROVED);
        // Set all lines as fully received
        purchaseOrder.getLines().get(0).setReceivedQuantity(BigDecimal.TEN);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(purchaseOrderRepository.findByIdWithLinesAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(purchaseOrder));

        // When/Then
        assertThrows(BusinessException.class, () -> purchaseOrderService.receivePurchaseOrder(1L));
    }
}
