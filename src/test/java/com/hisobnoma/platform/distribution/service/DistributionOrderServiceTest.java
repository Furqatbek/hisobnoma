package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.*;
import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderLine;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.entity.DistributionPaymentMethod;
import com.hisobnoma.platform.distribution.mapper.DistributionOrderMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionOrderRepository;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.pos.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionOrderServiceTest {

    @Mock private DistributionOrderRepository orderRepository;
    @Mock private DistributionOrderMapper orderMapper;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private com.hisobnoma.platform.distribution.repository.DistributionRouteRepository routeRepository;
    @Mock private com.hisobnoma.platform.distribution.repository.DistributionVisitRepository visitRepository;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private PricingService pricingService;
    @Mock private StockService stockService;
    @Mock private DistributionStockService distributionStockService;
    @Mock private ARInvoiceService arInvoiceService;

    @InjectMocks private DistributionOrderService service;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        lenient().when(orderMapper.toDto(any(DistributionOrder.class))).thenReturn(new DistributionOrderDto());
    }

    private Customer customer() {
        Customer c = Customer.builder().code("C-1").name("Osiyo Savdo").paymentTermsDays(14)
                .defaultCurrency("UZS").priceListId(77L).build();
        c.setId(100L);
        return c;
    }

    private Product product(long id, String name, BigDecimal price) {
        Product p = Product.builder().sku("SKU-" + id).name(name).sellingPrice(price).build();
        p.setId(id);
        return p;
    }

    private DistributionOrder orderInStatus(DistributionOrderStatus status, Long sourceLocationId) {
        DistributionOrder o = DistributionOrder.builder()
                .orderNumber("DO20260706-00001")
                .status(status)
                .customerId(100L)
                .customerName("Osiyo Savdo")
                .sourceLocationId(sourceLocationId)
                .paymentMethod(DistributionPaymentMethod.CREDIT)
                .orderDate(LocalDate.of(2026, 7, 6))
                .totalAmount(new BigDecimal("240000"))
                .creditAmount(new BigDecimal("240000"))
                .tenantId(TENANT_ID)
                .lines(new ArrayList<>())
                .build();
        o.setId(50L);
        DistributionOrderLine line = DistributionOrderLine.builder()
                .productId(10L).productName("Cola").quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("120000")).lineTotal(new BigDecimal("240000"))
                .tenantId(TENANT_ID).build();
        o.addLine(line);
        return o;
    }

    // ---- create ----

    @Test
    void createOrder_snapshotsServerSidePricesAndTotals() {
        CreateDistributionOrderRequest request = CreateDistributionOrderRequest.builder()
                .customerId(100L)
                .deliveryFee(new BigDecimal("15000"))
                .lines(List.of(
                        DistributionOrderLineRequest.builder().productId(10L).quantity(new BigDecimal("2")).build(),
                        DistributionOrderLineRequest.builder().productId(11L).quantity(new BigDecimal("1"))
                                .discountPercent(new BigDecimal("10")).build()))
                .build();

        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer()));
        when(productRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(product(10L, "Cola", new BigDecimal("12000"))));
        when(productRepository.findByIdAndTenantId(11L, TENANT_ID)).thenReturn(Optional.of(product(11L, "Juice", new BigDecimal("8000"))));
        // ignore client price; server resolves
        when(pricingService.getProductPrice(eq(10L), isNull(), any(), eq(100L), isNull(), eq(TENANT_ID))).thenReturn(new BigDecimal("12000"));
        when(pricingService.getProductPrice(eq(11L), isNull(), any(), eq(100L), isNull(), eq(TENANT_ID))).thenReturn(new BigDecimal("8000"));
        when(orderRepository.findMaxOrderNumberByPrefix(eq(TENANT_ID), anyString())).thenReturn(null);
        when(orderRepository.existsByTenantIdAndOrderNumber(eq(TENANT_ID), anyString())).thenReturn(false);
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createOrder(request);

        ArgumentCaptor<DistributionOrder> captor = ArgumentCaptor.forClass(DistributionOrder.class);
        verify(orderRepository).save(captor.capture());
        DistributionOrder saved = captor.getValue();

        assertEquals(DistributionOrderStatus.DRAFT, saved.getStatus());
        assertTrue(saved.getOrderNumber().startsWith("DO"));
        assertEquals(2, saved.getLines().size());
        // line 1: 2 * 12000 = 24000 ; line 2: 1 * 8000 - 10% = 7200
        assertEquals(0, new BigDecimal("24000").compareTo(saved.getLines().get(0).getLineTotal()));
        assertEquals(0, new BigDecimal("7200").compareTo(saved.getLines().get(1).getLineTotal()));
        // subtotal 31200 + deliveryFee 15000 = 46200
        assertEquals(0, new BigDecimal("31200").compareTo(saved.getSubtotal()));
        assertEquals(0, new BigDecimal("46200").compareTo(saved.getTotalAmount()));
        // due date = orderDate (today) + customer terms (14)
        assertEquals(LocalDate.now().plusDays(14), saved.getDueDate());
        // pricing basis snapshotted from the customer's price list
        assertEquals(77L, saved.getPriceListId());
    }

    @Test
    void createOrder_customerNotFound_throws() {
        CreateDistributionOrderRequest request = CreateDistributionOrderRequest.builder()
                .customerId(999L)
                .lines(List.of(DistributionOrderLineRequest.builder().productId(10L).quantity(BigDecimal.ONE).build()))
                .build();
        when(customerRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_unknownAgent_throws() {
        CreateDistributionOrderRequest request = CreateDistributionOrderRequest.builder()
                .customerId(100L).agentId(7L)
                .lines(List.of(DistributionOrderLineRequest.builder().productId(10L).quantity(BigDecimal.ONE).build()))
                .build();
        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer()));
        when(agentRepository.findByIdAndTenantId(7L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createOrder(request));
    }

    // ---- update ----

    @Test
    void updateOrder_nonDraft_throws() {
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID))
                .thenReturn(Optional.of(orderInStatus(DistributionOrderStatus.CONFIRMED, 5L)));

        assertThrows(BusinessException.class,
                () -> service.updateOrder(50L, UpdateDistributionOrderRequest.builder().notes("x").build()));
    }

    // ---- confirm ----

    @Test
    void confirm_reservesStockAtResolvedDefaultLocation() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.DRAFT, null);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        Location def = Location.builder().code("WH").name("Main").build();
        def.setId(9L);
        when(locationRepository.findByTenantIdAndIsDefaultTrue(TENANT_ID)).thenReturn(Optional.of(def));

        service.confirm(50L);

        assertEquals(DistributionOrderStatus.CONFIRMED, order.getStatus());
        assertEquals(9L, order.getSourceLocationId());
        verify(stockService).reserveStock(eq(10L), eq(9L), eq(new BigDecimal("2")),
                eq(MovementReferenceType.DISTRIBUTION_ORDER), eq(50L), anyString());
    }

    @Test
    void confirm_invalidTransition_throws() {
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID))
                .thenReturn(Optional.of(orderInStatus(DistributionOrderStatus.DELIVERED, 9L)));

        assertThrows(BusinessException.class, () -> service.confirm(50L));
        verify(stockService, never()).reserveStock(any(), any(), any(), any(), any(), any());
    }

    // ---- deliver ----

    @Test
    void deliver_deductsStockReleasesReservationAndSplitsCash() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.IN_TRANSIT, 9L);
        order.setPaymentMethod(DistributionPaymentMethod.CASH);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deliver(50L, DeliverDistributionOrderRequest.builder().cashCollected(new BigDecimal("240000")).build());

        assertEquals(DistributionOrderStatus.DELIVERED, order.getStatus());
        assertNotNull(order.getDeliveredAt());
        assertEquals(0, new BigDecimal("240000").compareTo(order.getCashCollected()));
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getCreditAmount()));
        assertEquals(0, order.getLines().get(0).getQuantity().compareTo(order.getLines().get(0).getFulfilledQuantity()));
        verify(stockService).releaseReservation(10L, 9L, MovementReferenceType.DISTRIBUTION_ORDER, 50L);
        verify(distributionStockService).deduct(eq(10L), eq(9L), eq(new BigDecimal("2")), eq(50L), anyString());
    }

    @Test
    void deliver_stockDeductFailure_marksUnsettledButStillDelivers() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.IN_TRANSIT, 9L);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("insufficient van stock"))
                .when(distributionStockService).deduct(any(), any(), any(), any(), any());

        service.deliver(50L, DeliverDistributionOrderRequest.builder().build());

        // Delivery still records (goods physically moved) but the stock gap is surfaced, not silent.
        assertEquals(DistributionOrderStatus.DELIVERED, order.getStatus());
        assertFalse(order.isStockSettled());
    }

    @Test
    void deliver_creditOrderCollectsNoCash() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.IN_TRANSIT, 9L);
        order.setPaymentMethod(DistributionPaymentMethod.CREDIT);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deliver(50L, DeliverDistributionOrderRequest.builder().cashCollected(new BigDecimal("100000")).build());

        assertEquals(0, BigDecimal.ZERO.compareTo(order.getCashCollected()));
        assertEquals(0, new BigDecimal("240000").compareTo(order.getCreditAmount()));
    }

    // ---- invoice ----

    @Test
    void invoice_createsArInvoiceLinkedToOrderAndTotalsMatch() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.DELIVERED, 9L);
        order.setDeliveryFee(new BigDecimal("10000"));
        order.setTotalAmount(new BigDecimal("250000"));
        order.setCreditAmount(new BigDecimal("250000")); // credit order → receivable raised
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        ARInvoiceDto invoiceDto = ARInvoiceDto.builder().id(777L).invoiceNumber("INV-000777").build();
        when(arInvoiceService.createInvoice(any(CreateARInvoiceRequest.class))).thenReturn(invoiceDto);

        service.invoice(50L);

        assertEquals(DistributionOrderStatus.INVOICED, order.getStatus());
        assertEquals(777L, order.getArInvoiceId());
        assertEquals("INV-000777", order.getArInvoiceNumber());

        ArgumentCaptor<CreateARInvoiceRequest> captor = ArgumentCaptor.forClass(CreateARInvoiceRequest.class);
        verify(arInvoiceService).createInvoice(captor.capture());
        CreateARInvoiceRequest req = captor.getValue();
        assertEquals(50L, req.getSalesOrderId());
        assertEquals(100L, req.getCustomerId());
        assertEquals(0, new BigDecimal("250000").compareTo(req.getTotalAmount()));
        // product line + delivery-fee line (no tax line since tax = 0)
        assertEquals(2, req.getLines().size());
        assertTrue(req.getLines().stream().anyMatch(l -> "Yetkazib berish".equals(l.getDescription())));
    }

    @Test
    void invoice_fullyCashSettledOrder_raisesNoReceivable() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.DELIVERED, 9L);
        order.setPaymentMethod(DistributionPaymentMethod.CASH);
        order.setCashCollected(new BigDecimal("240000"));
        order.setCreditAmount(BigDecimal.ZERO); // nothing owed on account
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.invoice(50L);

        assertEquals(DistributionOrderStatus.INVOICED, order.getStatus());
        assertNull(order.getArInvoiceId());
        verify(arInvoiceService, never()).createInvoice(any());
    }

    @Test
    void invoice_beforeDelivery_throws() {
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID))
                .thenReturn(Optional.of(orderInStatus(DistributionOrderStatus.CONFIRMED, 9L)));

        assertThrows(BusinessException.class, () -> service.invoice(50L));
        verify(arInvoiceService, never()).createInvoice(any());
    }

    // ---- cancel ----

    @Test
    void cancel_confirmedOrderReleasesReservation() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.CONFIRMED, 9L);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(50L, CancelDistributionOrderRequest.builder().reason("customer declined").build());

        assertEquals(DistributionOrderStatus.CANCELLED, order.getStatus());
        assertEquals("customer declined", order.getCancellationReason());
        verify(stockService).releaseReservation(10L, 9L, MovementReferenceType.DISTRIBUTION_ORDER, 50L);
    }

    @Test
    void cancel_draftOrderDoesNotTouchStock() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.DRAFT, null);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(50L, null);

        assertEquals(DistributionOrderStatus.CANCELLED, order.getStatus());
        verify(stockService, never()).releaseReservation(any(), any(), any(), any());
    }

    @Test
    void cancel_invoicedOrder_throws() {
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID))
                .thenReturn(Optional.of(orderInStatus(DistributionOrderStatus.INVOICED, 9L)));

        assertThrows(BusinessException.class, () -> service.cancel(50L, null));
    }

    // ---- delete ----

    @Test
    void deleteOrder_nonDraft_throws() {
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID))
                .thenReturn(Optional.of(orderInStatus(DistributionOrderStatus.CONFIRMED, 9L)));

        assertThrows(BusinessException.class, () -> service.deleteOrder(50L));
        verify(orderRepository, never()).delete(any());
    }

    @Test
    void deleteOrder_draft_deletes() {
        DistributionOrder order = orderInStatus(DistributionOrderStatus.DRAFT, null);
        when(orderRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(order));

        service.deleteOrder(50L);

        verify(orderRepository).delete(order);
    }
}
