package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebOrderPublicServiceTest {

    @Mock private WebOrderRepository orderRepository;
    @Mock private WebCatalogItemRepository catalogRepository;
    @Mock private DeliveryRegionRepository regionRepository;
    @Mock private DeliveryVillageRepository villageRepository;
    @Mock private CheckoutRateLimiter rateLimiter;
    @Mock private TelegramNotificationService telegramNotificationService;
    @Mock private WebPricingService pricingService;
    @Mock private WebCouponService couponService;

    @InjectMocks
    private WebOrderPublicService service;

    private static final Long TENANT_ID = 1L;

    private WebCatalogItem liveItem;

    @BeforeEach
    void setUp() {
        // Default: no promotions apply — checkout keeps undiscounted totals
        lenient().when(pricingService.priceResolved(anyList(), anyLong(), anyString()))
                .thenAnswer(inv -> {
                    List<WebPricingService.PricedLine> lines = inv.getArgument(0);
                    BigDecimal subtotal = lines.stream()
                            .map(WebPricingService.PricedLine::lineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new WebPricingService.CartPrice(lines, subtotal, BigDecimal.ZERO, List.of());
                });
        Product product = Product.builder()
                .sku("SKU-001").name("Cola")
                .sellingPrice(new BigDecimal("12000"))
                .active(true).sellable(true)
                .tenantId(TENANT_ID)
                .build();
        product.setId(10L);

        liveItem = WebCatalogItem.builder()
                .product(product).status(WebCatalogStatus.LIVE).sortOrder(1).tenantId(TENANT_ID)
                .build();
        liveItem.setId(100L);
    }

    private CheckoutRequest checkoutRequest(BigDecimal quantity) {
        return CheckoutRequest.builder()
                .customerName("Ali Valiyev")
                .phone("+998901234567")
                .lines(List.of(CheckoutRequest.CheckoutLine.builder()
                        .catalogItemId(100L).quantity(quantity).build()))
                .build();
    }

    // ---- checkout ----

    @Test
    void checkout_snapshotsServerSidePriceAndComputesTotal() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any())).thenReturn(0L);
        when(orderRepository.existsByTenantIdAndOrderNumber(TENANT_ID, "WO-000001")).thenReturn(false);
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicOrderDto dto = service.checkout(checkoutRequest(new BigDecimal("3")), "1.2.3.4", "app/1.0");

        assertEquals("WO-000001", dto.getOrderNumber());
        assertEquals(WebOrderStatus.NEW.name(), dto.getStatus());
        assertEquals(0, new BigDecimal("36000").compareTo(dto.getTotalAmount()));
        assertEquals(0, new BigDecimal("12000").compareTo(dto.getLines().get(0).getUnitPrice()));
    }

    @Test
    void checkout_usesPriceOverrideWhenSet() {
        liveItem.setPriceOverride(new BigDecimal("9999"));
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicOrderDto dto = service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null);

        assertEquals(0, new BigDecimal("9999").compareTo(dto.getTotalAmount()));
    }

    @Test
    void checkout_appliesPromotionDiscountFromEngine() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pricingService.priceResolved(anyList(), anyLong(), anyString()))
                .thenAnswer(inv -> {
                    List<WebPricingService.PricedLine> lines = inv.getArgument(0);
                    return new WebPricingService.CartPrice(lines, new BigDecimal("36000"),
                            new BigDecimal("3600"),
                            List.of(com.hisobnoma.platform.pos.dto.PriceCalculationResult.AppliedPromotion.builder()
                                    .promotionCode("WEB10").promotionName("10% off")
                                    .promotionType("PERCENTAGE_OFF")
                                    .discountAmount(new BigDecimal("3600"))
                                    .build()));
                });

        PublicOrderDto dto = service.checkout(checkoutRequest(new BigDecimal("3")), "1.2.3.4", null);

        assertEquals(0, new BigDecimal("3600").compareTo(dto.getDiscountTotal()));
        assertEquals(0, new BigDecimal("32400").compareTo(dto.getTotalAmount()));

        ArgumentCaptor<WebOrder> captor = ArgumentCaptor.forClass(WebOrder.class);
        verify(orderRepository).save(captor.capture());
        assertEquals("WEB10", captor.getValue().getAppliedPromotions());
        // Line snapshots stay at full price — the discount lives at order level
        assertEquals(0, new BigDecimal("12000").compareTo(captor.getValue().getLines().get(0).getUnitPrice()));
    }

    @Test
    void checkout_validCouponSnapshotsCodeAndDiscount() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(couponService.validate(eq("WELCOME"), any(), eq(TENANT_ID), anyString()))
                .thenReturn(new WebCouponService.CouponOutcome(true, new BigDecimal("5000")));

        CheckoutRequest request = checkoutRequest(new BigDecimal("3"));
        request.setCouponCode("WELCOME");

        PublicOrderDto dto = service.checkout(request, "1.2.3.4", null);

        assertEquals("WELCOME", dto.getCouponCode());
        assertEquals(0, new BigDecimal("5000").compareTo(dto.getCouponDiscount()));
        assertEquals(0, new BigDecimal("31000").compareTo(dto.getTotalAmount())); // 36000 - 5000
        // Coupon discounts the goods total after automatic promotions
        verify(couponService).validate(eq("WELCOME"), eq(new BigDecimal("36000")),
                eq(TENANT_ID), anyString());
    }

    @Test
    void checkout_invalidCouponRejectsCheckout() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(couponService.validate(eq("BOGUS"), any(), eq(TENANT_ID), anyString()))
                .thenReturn(WebCouponService.CouponOutcome.invalid());

        CheckoutRequest request = checkoutRequest(BigDecimal.ONE);
        request.setCouponCode("BOGUS");

        assertThrows(com.hisobnoma.platform.common.exception.ValidationException.class,
                () -> service.checkout(request, "1.2.3.4", null));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_withoutCouponNeverCallsCouponService() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null);

        verifyNoInteractions(couponService);
    }

    @Test
    void checkout_promotionEngineFailureNeverBlocksCheckout() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pricingService.priceResolved(anyList(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("engine down"));

        PublicOrderDto dto = service.checkout(checkoutRequest(new BigDecimal("3")), "1.2.3.4", null);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getDiscountTotal()));
        assertEquals(0, new BigDecimal("36000").compareTo(dto.getTotalAmount()));
    }

    @Test
    void checkout_rejectsDraftCatalogItem() {
        liveItem.setStatus(WebCatalogStatus.DRAFT);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));

        assertThrows(ValidationException.class,
                () -> service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_rejectsInactiveProduct() {
        liveItem.getProduct().setActive(false);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));

        assertThrows(ValidationException.class,
                () -> service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null));
    }

    @Test
    void checkout_rejectsUnknownCatalogItem() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null));
    }

    @Test
    void checkout_throws429WhenRateLimited() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        assertThrows(TooManyRequestsException.class,
                () -> service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_notifiesStaffExactlyOnce() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null);

        verify(telegramNotificationService, times(1))
                .sendBroadcastAlert(eq(TENANT_ID), any(), anyString(), anyString());
    }

    @Test
    void checkout_notificationFailureDoesNotBreakCheckout() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("telegram down")).when(telegramNotificationService)
                .sendBroadcastAlert(any(), any(), anyString(), anyString());

        PublicOrderDto dto = service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null);

        assertNotNull(dto.getOrderNumber());
    }

    @Test
    void checkout_generatesNextFreeOrderNumber() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any())).thenReturn(7L);
        when(orderRepository.existsByTenantIdAndOrderNumber(TENANT_ID, "WO-000008")).thenReturn(true);
        when(orderRepository.existsByTenantIdAndOrderNumber(TENANT_ID, "WO-000009")).thenReturn(false);
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicOrderDto dto = service.checkout(checkoutRequest(BigDecimal.ONE), "1.2.3.4", null);

        assertEquals("WO-000009", dto.getOrderNumber());
    }

    @Test
    void checkout_addsRegionDeliveryFeeToTotal() {
        com.hisobnoma.platform.delivery.entity.DeliveryRegion region =
                com.hisobnoma.platform.delivery.entity.DeliveryRegion.builder()
                        .name("Тошкент").deliveryFee(new BigDecimal("5000"))
                        .tenantId(TENANT_ID).build();
        region.setId(3L);
        when(regionRepository.findByIdAndTenantId(3L, TENANT_ID)).thenReturn(Optional.of(region));
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(orderRepository.save(any(WebOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutRequest request = checkoutRequest(new BigDecimal("3"));
        request.setRegionId(3L);

        PublicOrderDto dto = service.checkout(request, "1.2.3.4", null);

        assertEquals(0, new BigDecimal("5000").compareTo(dto.getDeliveryFee()));
        assertEquals(0, new BigDecimal("41000").compareTo(dto.getTotalAmount())); // 36000 + 5000
    }

    @Test
    void getRegions_includesDeliveryFee() {
        com.hisobnoma.platform.delivery.entity.DeliveryRegion region =
                com.hisobnoma.platform.delivery.entity.DeliveryRegion.builder()
                        .name("Тошкент").deliveryFee(new BigDecimal("7000"))
                        .tenantId(TENANT_ID).build();
        region.setId(3L);
        when(regionRepository.findActiveByTenantId(TENANT_ID)).thenReturn(List.of(region));

        var regions = service.getRegions();

        assertEquals(0, new BigDecimal("7000").compareTo(regions.get(0).getDeliveryFee()));
    }

    // ---- status lookup ----

    @Test
    void getOrderStatus_returnsOrderForMatchingPhone() {
        WebOrder order = WebOrder.builder()
                .orderNumber("WO-000001").status(WebOrderStatus.CONFIRMED)
                .customerName("Ali").phone("+998 90 123-45-67")
                .totalAmount(new BigDecimal("12000")).tenantId(TENANT_ID)
                .build();
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, "WO-000001"))
                .thenReturn(Optional.of(order));

        PublicOrderDto dto = service.getOrderStatus("WO-000001", "998901234567");

        assertEquals("CONFIRMED", dto.getStatus());
    }

    @Test
    void getOrderStatus_wrongPhoneThrowsNotFound() {
        WebOrder order = WebOrder.builder()
                .orderNumber("WO-000001").status(WebOrderStatus.NEW)
                .customerName("Ali").phone("+998901234567")
                .totalAmount(BigDecimal.TEN).tenantId(TENANT_ID)
                .build();
        when(orderRepository.findByTenantIdAndOrderNumber(TENANT_ID, "WO-000001"))
                .thenReturn(Optional.of(order));

        assertThrows(NotFoundException.class,
                () -> service.getOrderStatus("WO-000001", "+998907777777"));
    }

    // ---- phone helpers ----

    @Test
    void phoneMatches_normalizesFormatting() {
        assertTrue(WebOrderPublicService.phoneMatches("+998 90 123-45-67", "998901234567"));
        assertTrue(WebOrderPublicService.phoneMatches("998901234567", "90 123 45 67"));
        assertFalse(WebOrderPublicService.phoneMatches("+998901234567", "+998907654321"));
        assertFalse(WebOrderPublicService.phoneMatches("+998901234567", "123"));
    }
}
