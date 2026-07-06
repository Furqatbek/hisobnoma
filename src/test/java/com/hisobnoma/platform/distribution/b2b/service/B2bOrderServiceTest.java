package com.hisobnoma.platform.distribution.b2b.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.b2b.dto.B2bPlaceOrderRequest;
import com.hisobnoma.platform.distribution.dto.DistributionOrderDto;
import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.mapper.DistributionOrderMapper;
import com.hisobnoma.platform.distribution.repository.DistributionOrderRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.pos.service.PricingService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class B2bOrderServiceTest {

    @Mock private DistributionOrderRepository orderRepository;
    @Mock private DistributionOrderMapper orderMapper;
    @Mock private ProductRepository productRepository;
    @Mock private PricingService pricingService;

    @InjectMocks private B2bOrderService service;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(orderMapper.toDto(any(DistributionOrder.class))).thenReturn(new DistributionOrderDto());
    }

    private Customer buyer(boolean creditHold) {
        Customer c = Customer.builder().code("C-1").name("Osiyo").defaultCurrency("UZS")
                .paymentTermsDays(14).creditHold(creditHold).build();
        c.setId(100L);
        c.setTenantId(TENANT_ID);
        return c;
    }

    private Product product(long id) {
        Product p = Product.builder().sku("SKU-" + id).name("P" + id).sellingPrice(new BigDecimal("1000"))
                .active(true).sellable(true).build();
        p.setId(id);
        return p;
    }

    @Test
    void placeOrder_pricesServerSideAndCreatesDraft() {
        B2bPlaceOrderRequest request = B2bPlaceOrderRequest.builder()
                .deliveryAddress("Chilonzor 5")
                .lines(List.of(
                        B2bPlaceOrderRequest.Line.builder().productId(10L).quantity(new BigDecimal("3")).build(),
                        B2bPlaceOrderRequest.Line.builder().productId(11L).quantity(new BigDecimal("2")).build()))
                .build();

        when(productRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(product(10L)));
        when(productRepository.findByIdAndTenantId(11L, TENANT_ID)).thenReturn(Optional.of(product(11L)));
        // server prices, ignoring any client input
        when(pricingService.getProductPrice(eq(10L), isNull(), any(), eq(100L), isNull(), eq(TENANT_ID))).thenReturn(new BigDecimal("900"));
        when(pricingService.getProductPrice(eq(11L), isNull(), any(), eq(100L), isNull(), eq(TENANT_ID))).thenReturn(new BigDecimal("1500"));
        when(orderRepository.findMaxOrderNumberByPrefix(eq(TENANT_ID), anyString())).thenReturn(null);
        when(orderRepository.existsByTenantIdAndOrderNumber(eq(TENANT_ID), anyString())).thenReturn(false);
        when(orderRepository.save(any(DistributionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.placeOrder(buyer(false), request);

        ArgumentCaptor<DistributionOrder> captor = ArgumentCaptor.forClass(DistributionOrder.class);
        verify(orderRepository).save(captor.capture());
        DistributionOrder saved = captor.getValue();

        assertEquals(DistributionOrderStatus.DRAFT, saved.getStatus());
        assertEquals(100L, saved.getCustomerId());
        assertTrue(saved.getOrderNumber().startsWith("DO"));
        assertEquals(2, saved.getLines().size());
        // line 1: 3 * 900 = 2700 ; line 2: 2 * 1500 = 3000 ; subtotal 5700
        assertEquals(0, new BigDecimal("2700").compareTo(saved.getLines().get(0).getLineTotal()));
        assertEquals(0, new BigDecimal("5700").compareTo(saved.getTotalAmount()));
        assertEquals("Chilonzor 5", saved.getDeliveryAddress());
    }

    @Test
    void placeOrder_creditHold_throws() {
        B2bPlaceOrderRequest request = B2bPlaceOrderRequest.builder()
                .lines(List.of(B2bPlaceOrderRequest.Line.builder().productId(10L).quantity(BigDecimal.ONE).build()))
                .build();

        assertThrows(BusinessException.class, () -> service.placeOrder(buyer(true), request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_unavailableProduct_throws() {
        Product inactive = product(10L);
        inactive.setSellable(false);
        when(productRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(inactive));

        B2bPlaceOrderRequest request = B2bPlaceOrderRequest.builder()
                .lines(List.of(B2bPlaceOrderRequest.Line.builder().productId(10L).quantity(BigDecimal.ONE).build()))
                .build();

        assertThrows(BusinessException.class, () -> service.placeOrder(buyer(false), request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_belongingToAnotherCustomer_throwsNotFound() {
        DistributionOrder other = DistributionOrder.builder()
                .orderNumber("DO20260706-00001").customerId(200L).tenantId(TENANT_ID).build();
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, "DO20260706-00001")).thenReturn(Optional.of(other));

        assertThrows(NotFoundException.class, () -> service.getOrder(buyer(false), "DO20260706-00001"));
    }

    @Test
    void getOrder_ownOrder_returnsDto() {
        DistributionOrder own = DistributionOrder.builder()
                .orderNumber("DO20260706-00002").customerId(100L).tenantId(TENANT_ID).build();
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, "DO20260706-00002")).thenReturn(Optional.of(own));

        assertNotNull(service.getOrder(buyer(false), "DO20260706-00002"));
    }
}
