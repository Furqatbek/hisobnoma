package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.CustomerService;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.dto.WebOrderDto;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
