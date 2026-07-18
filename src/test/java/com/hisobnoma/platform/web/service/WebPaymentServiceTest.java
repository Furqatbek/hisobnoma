package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.web.dto.CreatePaymentRequest;
import com.hisobnoma.platform.web.dto.PaymentDto;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.entity.WebPayment;
import com.hisobnoma.platform.web.entity.WebPaymentProviderType;
import com.hisobnoma.platform.web.entity.WebPaymentStatus;
import com.hisobnoma.platform.web.exception.PaymentNotConfiguredException;
import com.hisobnoma.platform.web.payment.PaymentProvider;
import com.hisobnoma.platform.web.payment.PaymentProviderRegistry;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import com.hisobnoma.platform.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPaymentServiceTest {

    @Mock private WebPaymentRepository paymentRepository;
    @Mock private WebOrderRepository orderRepository;
    @Mock private PaymentProviderRegistry providerRegistry;
    @Mock private PaymentProvider paymeProvider;

    @InjectMocks
    private WebPaymentService service;

    private static final Long TENANT_ID = 1L; // TenantContext default in tests
    private static final String ORDER_NUMBER = "WO-000001";
    private static final String PHONE = "+998901234567";

    private WebOrder order;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID); // service fails closed without a tenant
        order = WebOrder.builder()
                .tenantId(TENANT_ID).orderNumber(ORDER_NUMBER).phone(PHONE)
                .status(WebOrderStatus.NEW)
                .totalAmount(new BigDecimal("33000"))
                .currency("UZS")
                .build();
        order.setId(5L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CreatePaymentRequest request(String provider) {
        return CreatePaymentRequest.builder().phone(PHONE).provider(provider).build();
    }

    @Test
    void createPayment_unconfiguredProvider_throws503() {
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, ORDER_NUMBER))
                .thenReturn(Optional.of(order));
        when(providerRegistry.get(WebPaymentProviderType.PAYME)).thenReturn(paymeProvider);
        when(paymeProvider.isConfigured()).thenReturn(false);

        assertThrows(PaymentNotConfiguredException.class,
                () -> service.createPayment(ORDER_NUMBER, request("PAYME")));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_configuredProvider_returnsPendingWithProviderUrl() {
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, ORDER_NUMBER))
                .thenReturn(Optional.of(order));
        when(providerRegistry.get(WebPaymentProviderType.PAYME)).thenReturn(paymeProvider);
        when(paymeProvider.isConfigured()).thenReturn(true);
        when(paymeProvider.createSession(any())).thenReturn(
                new PaymentProvider.Session("https://checkout.paycom.uz/abc123", null));
        when(paymentRepository.save(any(WebPayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto dto = service.createPayment(ORDER_NUMBER, request("PAYME"));

        assertTrue(dto.getId().startsWith("pay_"));
        assertEquals("https://checkout.paycom.uz/abc123", dto.getPaymentUrl());
        assertTrue(dto.getPaymentUrl().startsWith("https://"));
        assertEquals("PENDING", dto.getStatus());
        assertEquals("PAYME", dto.getProvider());
        assertEquals(0, new BigDecimal("33000").compareTo(dto.getAmount()));
        // The order is flagged awaiting card payment.
        assertEquals(WebPaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void createPayment_alreadyPaid_rejected() {
        order.setPaymentStatus(WebPaymentStatus.PAID);
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, ORDER_NUMBER))
                .thenReturn(Optional.of(order));

        assertThrows(ValidationException.class,
                () -> service.createPayment(ORDER_NUMBER, request("PAYME")));
    }

    @Test
    void createPayment_wrongPhone_notFound() {
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, ORDER_NUMBER))
                .thenReturn(Optional.of(order));
        CreatePaymentRequest wrong = CreatePaymentRequest.builder()
                .phone("+998900000000").provider("PAYME").build();

        assertThrows(NotFoundException.class, () -> service.createPayment(ORDER_NUMBER, wrong));
    }

    @Test
    void getStatus_returnsDto() {
        WebPayment payment = WebPayment.builder()
                .tenantId(TENANT_ID).webOrderId(5L).orderNumber(ORDER_NUMBER)
                .publicId("pay_deadbeef").provider(WebPaymentProviderType.CLICK)
                .status(WebPaymentStatus.PENDING).amount(new BigDecimal("33000")).currency("UZS")
                .paymentUrl("https://my.click.uz/pay").build();
        when(paymentRepository.findByPublicIdAndTenantId("pay_deadbeef", TENANT_ID))
                .thenReturn(Optional.of(payment));

        PaymentDto dto = service.getStatus("pay_deadbeef");

        assertEquals("pay_deadbeef", dto.getId());
        assertEquals("CLICK", dto.getProvider());
        assertEquals("PENDING", dto.getStatus());
    }

    @Test
    void markPaid_updatesPaymentAndOrder_andIsIdempotent() {
        WebPayment payment = WebPayment.builder()
                .tenantId(TENANT_ID).webOrderId(5L).orderNumber(ORDER_NUMBER)
                .publicId("pay_x").provider(WebPaymentProviderType.PAYME)
                .status(WebPaymentStatus.PENDING).amount(new BigDecimal("33000")).build();
        when(paymentRepository.findByProviderAndProviderReference(WebPaymentProviderType.PAYME, "ref-1"))
                .thenReturn(Optional.of(payment));
        when(orderRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(order));

        service.markPaid(WebPaymentProviderType.PAYME, "ref-1");
        assertEquals(WebPaymentStatus.PAID, payment.getStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals(WebPaymentStatus.PAID, order.getPaymentStatus());

        // Second call is a no-op (already PAID): order is not saved again.
        service.markPaid(WebPaymentProviderType.PAYME, "ref-1");
        verify(orderRepository, times(1)).save(order);
    }
}
