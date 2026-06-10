package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.CustomerService;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.Stock;
import com.hisobnoma.platform.inventory.entity.StockReservation;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.inventory.repository.StockReservationRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.dto.WebOrderDto;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebOrderServiceTest {

    @Mock private WebOrderRepository orderRepository;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private CustomerService customerService;
    @Mock private ARInvoiceService arInvoiceService;
    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private StockRepository stockRepository;
    @Mock private StockReservationRepository stockReservationRepository;
    @Mock private StockService stockService;

    @InjectMocks
    private WebOrderService service;

    private static final Long TENANT_ID = 1L;

    private WebOrder order;

    @BeforeEach
    void setUp() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);

        order = WebOrder.builder()
                .orderNumber("WO-000001")
                .status(WebOrderStatus.NEW)
                .customerName("Ali Valiyev")
                .phone("+998901234567")
                .deliveryRegionName("Тошкент")
                .deliveryVillageName("Чилонзор")
                .totalAmount(new BigDecimal("36000"))
                .tenantId(TENANT_ID)
                .build();
        order.setId(1L);
        order.addLine(WebOrderLine.builder()
                .productId(10L).productName("Cola").unitName("dona")
                .quantity(new BigDecimal("3"))
                .unitPrice(new BigDecimal("12000"))
                .lineTotal(new BigDecimal("36000"))
                .tenantId(TENANT_ID)
                .build());

        lenient().when(orderRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(order));
    }

    private UpdateOrderStatusRequest statusRequest(WebOrderStatus status, String reason) {
        return UpdateOrderStatusRequest.builder().status(status).reason(reason).build();
    }

    // ---- status transitions ----

    @Test
    void updateStatus_newToConfirmedIsAllowed() {
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WebOrderDto dto = service.updateStatus(1L, statusRequest(WebOrderStatus.CONFIRMED, null));

        assertEquals(WebOrderStatus.CONFIRMED, dto.getStatus());
    }

    @Test
    void updateStatus_newToCompletedIsRejected() {
        assertThrows(ValidationException.class,
                () -> service.updateStatus(1L, statusRequest(WebOrderStatus.COMPLETED, null)));
    }

    @Test
    void updateStatus_completedOrderCannotChange() {
        order.setStatus(WebOrderStatus.COMPLETED);

        assertThrows(ValidationException.class,
                () -> service.updateStatus(1L, statusRequest(WebOrderStatus.CANCELLED, "x")));
    }

    @Test
    void updateStatus_cancelRequiresReason() {
        assertThrows(ValidationException.class,
                () -> service.updateStatus(1L, statusRequest(WebOrderStatus.CANCELLED, "  ")));
    }

    @Test
    void updateStatus_cancelStoresReason() {
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WebOrderDto dto = service.updateStatus(1L,
                statusRequest(WebOrderStatus.CANCELLED, "Мижоз бекор қилди"));

        assertEquals(WebOrderStatus.CANCELLED, dto.getStatus());
        assertEquals("Мижоз бекор қилди", dto.getCancellationReason());
    }

    @Test
    void updateStatus_cancelledOrderCannotBeConfirmed() {
        order.setStatus(WebOrderStatus.CANCELLED);

        assertThrows(ValidationException.class,
                () -> service.updateStatus(1L, statusRequest(WebOrderStatus.CONFIRMED, null)));
    }

    // ---- stock reservation ----

    private Stock stock(BigDecimal onHand, BigDecimal reserved, Long locationId) {
        Location location = Location.builder().code("MAIN").name("Asosiy ombor").build();
        location.setId(locationId);
        Product product = Product.builder().sku("SKU-10").name("Cola")
                .sellingPrice(BigDecimal.TEN).build();
        product.setId(10L);
        Stock s = Stock.builder()
                .product(product).location(location)
                .quantityOnHand(onHand).quantityReserved(reserved)
                .tenantId(TENANT_ID).build();
        return s;
    }

    @Test
    void confirm_reservesStockAtLocationWithEnoughAvailability() {
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndTenantId(10L, TENANT_ID))
                .thenReturn(List.of(stock(new BigDecimal("100"), BigDecimal.ZERO, 5L)));

        service.updateStatus(1L, statusRequest(WebOrderStatus.CONFIRMED, null));

        verify(stockService).reserveStock(eq(10L), eq(5L), eq(new BigDecimal("3")),
                eq(MovementReferenceType.WEB_ORDER), eq(1L), eq("WO-000001"));
    }

    @Test
    void confirm_skipsReservationWhenStockInsufficientButStillConfirms() {
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndTenantId(10L, TENANT_ID))
                .thenReturn(List.of(stock(new BigDecimal("1"), BigDecimal.ZERO, 5L)));

        WebOrderDto dto = service.updateStatus(1L, statusRequest(WebOrderStatus.CONFIRMED, null));

        assertEquals(WebOrderStatus.CONFIRMED, dto.getStatus());
        verify(stockService, never()).reserveStock(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirm_reservationFailureNeverBlocksConfirmation() {
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndTenantId(10L, TENANT_ID))
                .thenThrow(new RuntimeException("db down"));

        WebOrderDto dto = service.updateStatus(1L, statusRequest(WebOrderStatus.CONFIRMED, null));

        assertEquals(WebOrderStatus.CONFIRMED, dto.getStatus());
    }

    @Test
    void cancelAfterConfirm_releasesActiveReservations() {
        order.setStatus(WebOrderStatus.CONFIRMED);
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        Stock s = stock(new BigDecimal("100"), new BigDecimal("3"), 5L);
        StockReservation active = StockReservation.builder()
                .product(s.getProduct()).location(s.getLocation())
                .quantity(new BigDecimal("3"))
                .referenceType(MovementReferenceType.WEB_ORDER).referenceId(1L)
                .status(StockReservation.ReservationStatus.ACTIVE)
                .tenantId(TENANT_ID).build();
        StockReservation cancelled = StockReservation.builder()
                .product(s.getProduct()).location(s.getLocation())
                .quantity(BigDecimal.ONE)
                .referenceType(MovementReferenceType.WEB_ORDER).referenceId(1L)
                .status(StockReservation.ReservationStatus.CANCELLED)
                .tenantId(TENANT_ID).build();
        when(stockReservationRepository.findByReferenceTypeAndReferenceIdAndTenantId(
                MovementReferenceType.WEB_ORDER, 1L, TENANT_ID))
                .thenReturn(List.of(active, cancelled));

        service.updateStatus(1L, statusRequest(WebOrderStatus.CANCELLED, "Бекор"));

        // Only the ACTIVE reservation is released, exactly once
        verify(stockService, times(1)).releaseReservation(
                eq(10L), eq(5L), eq(MovementReferenceType.WEB_ORDER), eq(1L));
    }

    @Test
    void completeAfterDelivering_releasesReservations() {
        order.setStatus(WebOrderStatus.DELIVERING);
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockReservationRepository.findByReferenceTypeAndReferenceIdAndTenantId(
                MovementReferenceType.WEB_ORDER, 1L, TENANT_ID))
                .thenReturn(List.of());

        service.updateStatus(1L, statusRequest(WebOrderStatus.COMPLETED, null));

        verify(stockReservationRepository).findByReferenceTypeAndReferenceIdAndTenantId(
                MovementReferenceType.WEB_ORDER, 1L, TENANT_ID);
    }

    @Test
    void cancelFromNew_neverTouchesReservations() {
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1L, statusRequest(WebOrderStatus.CANCELLED, "Бекор"));

        verifyNoInteractions(stockReservationRepository);
        verify(stockService, never()).releaseReservation(any(), any(), any(), any());
    }

    // ---- delivery fee ----

    @Test
    void convert_addsDeliveryFeeAsInvoiceLine() {
        order.setDeliveryFee(new BigDecimal("5000"));
        order.setTotalAmount(new BigDecimal("41000")); // 36000 lines + 5000 fee
        order.setCustomerId(88L);
        when(arInvoiceService.createInvoice(any(CreateARInvoiceRequest.class)))
                .thenReturn(ARInvoiceDto.builder().id(60L).invoiceNumber("INV-000060").build());
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.convertToInvoice(1L);

        ArgumentCaptor<CreateARInvoiceRequest> captor =
                ArgumentCaptor.forClass(CreateARInvoiceRequest.class);
        verify(arInvoiceService).createInvoice(captor.capture());
        CreateARInvoiceRequest request = captor.getValue();
        assertEquals(2, request.getLines().size());
        var feeLine = request.getLines().get(1);
        assertEquals("Етказиб бериш", feeLine.getProductName());
        assertEquals(0, new BigDecimal("5000").compareTo(feeLine.getUnitPrice()));
        assertEquals(0, BigDecimal.ONE.compareTo(feeLine.getQuantity()));
        assertEquals(0, new BigDecimal("41000").compareTo(request.getTotalAmount()));
    }

    @Test
    void convert_zeroFeeAddsNoExtraLine() {
        order.setCustomerId(88L);
        when(arInvoiceService.createInvoice(any(CreateARInvoiceRequest.class)))
                .thenReturn(ARInvoiceDto.builder().id(61L).invoiceNumber("INV-000061").build());
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.convertToInvoice(1L);

        ArgumentCaptor<CreateARInvoiceRequest> captor =
                ArgumentCaptor.forClass(CreateARInvoiceRequest.class);
        verify(arInvoiceService).createInvoice(captor.capture());
        assertEquals(1, captor.getValue().getLines().size());
    }

    // ---- convert to invoice ----

    @Test
    void convert_cancelledOrderIsRejected() {
        order.setStatus(WebOrderStatus.CANCELLED);

        assertThrows(ValidationException.class, () -> service.convertToInvoice(1L));
        verifyNoInteractions(arInvoiceService);
    }

    @Test
    void convert_alreadyConvertedOrderIsRejected() {
        order.setArInvoiceId(55L);
        order.setArInvoiceNumber("INV-000055");

        assertThrows(ValidationException.class, () -> service.convertToInvoice(1L));
        verifyNoInteractions(arInvoiceService);
    }

    @Test
    void convert_createsCustomerWhenNotLinkedAndBuildsInvoiceFromSnapshots() {
        when(customerService.createCustomer(any(CreateCustomerRequest.class)))
                .thenReturn(CustomerDto.builder().id(77L).code("CUST-000077").build());
        when(arInvoiceService.createInvoice(any(CreateARInvoiceRequest.class)))
                .thenReturn(ARInvoiceDto.builder().id(55L).invoiceNumber("INV-000055").build());
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WebOrderDto dto = service.convertToInvoice(1L);

        // Customer created from the order's contact data, with delivery address
        ArgumentCaptor<CreateCustomerRequest> customerCaptor =
                ArgumentCaptor.forClass(CreateCustomerRequest.class);
        verify(customerService).createCustomer(customerCaptor.capture());
        assertEquals("Ali Valiyev", customerCaptor.getValue().getName());
        assertEquals("+998901234567", customerCaptor.getValue().getPhone());
        assertEquals("Тошкент, Чилонзор", customerCaptor.getValue().getAddress());

        // Invoice request built from snapshotted lines
        ArgumentCaptor<CreateARInvoiceRequest> invoiceCaptor =
                ArgumentCaptor.forClass(CreateARInvoiceRequest.class);
        verify(arInvoiceService).createInvoice(invoiceCaptor.capture());
        CreateARInvoiceRequest invoiceRequest = invoiceCaptor.getValue();
        assertEquals(77L, invoiceRequest.getCustomerId());
        assertEquals(0, new BigDecimal("36000").compareTo(invoiceRequest.getTotalAmount()));
        assertEquals(1, invoiceRequest.getLines().size());
        assertEquals("Cola", invoiceRequest.getLines().get(0).getProductName());
        assertEquals(0, new BigDecimal("12000")
                .compareTo(invoiceRequest.getLines().get(0).getUnitPrice()));

        // Order linked to the created customer and invoice
        assertEquals(77L, dto.getCustomerId());
        assertEquals(55L, dto.getArInvoiceId());
        assertEquals("INV-000055", dto.getArInvoiceNumber());
    }

    @Test
    void convert_usesCustomerLinkedToWebAccountByPhone() {
        order.setPhoneNormalized("998901234567");
        WebCustomer webCustomer = WebCustomer.builder()
                .phone("998901234567").customerId(99L).tenantId(TENANT_ID).build();
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.of(webCustomer));
        when(arInvoiceService.createInvoice(any(CreateARInvoiceRequest.class)))
                .thenReturn(ARInvoiceDto.builder().id(57L).invoiceNumber("INV-000057").build());
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WebOrderDto dto = service.convertToInvoice(1L);

        // No new customer created — the staff-linked AR customer is used
        verify(customerService, never()).createCustomer(any());
        ArgumentCaptor<CreateARInvoiceRequest> captor =
                ArgumentCaptor.forClass(CreateARInvoiceRequest.class);
        verify(arInvoiceService).createInvoice(captor.capture());
        assertEquals(99L, captor.getValue().getCustomerId());
        assertEquals(99L, dto.getCustomerId());
    }

    @Test
    void convert_reusesLinkedCustomer() {
        order.setCustomerId(88L);
        when(arInvoiceService.createInvoice(any(CreateARInvoiceRequest.class)))
                .thenReturn(ARInvoiceDto.builder().id(56L).invoiceNumber("INV-000056").build());
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.convertToInvoice(1L);

        verify(customerService, never()).createCustomer(any());
        ArgumentCaptor<CreateARInvoiceRequest> captor =
                ArgumentCaptor.forClass(CreateARInvoiceRequest.class);
        verify(arInvoiceService).createInvoice(captor.capture());
        assertEquals(88L, captor.getValue().getCustomerId());
    }

    // ---- counts ----

    @Test
    void getNewOrderCount_delegatesToRepository() {
        when(orderRepository.countByTenantIdAndStatus(TENANT_ID, WebOrderStatus.NEW)).thenReturn(3L);

        assertEquals(3L, service.getNewOrderCount());
    }
}
