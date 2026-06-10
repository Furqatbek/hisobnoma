package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.service.PromotionService;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
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
class WebPricingServiceTest {

    @Mock private WebCatalogItemRepository catalogRepository;
    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private WebPricingService service;

    private static final Long TENANT_ID = 1L;

    private WebCatalogItem liveItem;

    @BeforeEach
    void setUp() {
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

    private List<CheckoutRequest.CheckoutLine> lines(BigDecimal quantity) {
        return List.of(CheckoutRequest.CheckoutLine.builder()
                .catalogItemId(100L).quantity(quantity).build());
    }

    private PromotionService.PromotionApplicationResult noPromotions() {
        return new PromotionService.PromotionApplicationResult(List.of(), List.of(), BigDecimal.ZERO);
    }

    private PromotionService.PromotionApplicationResult discount(String code, String name, BigDecimal amount) {
        return new PromotionService.PromotionApplicationResult(List.of(),
                List.of(PriceCalculationResult.AppliedPromotion.builder()
                        .promotionCode(code).promotionName(name)
                        .promotionType("PERCENTAGE_OFF")
                        .discountAmount(amount)
                        .build()),
                amount);
    }

    @Test
    void price_usesWebChannelAndEffectivePrice() {
        liveItem.setPriceOverride(new BigDecimal("9000"));
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB))).thenReturn(noPromotions());

        WebPricingService.CartPrice price = service.price(lines(new BigDecimal("2")), TENANT_ID, null);

        // The catalog price override wins as the base the engine discounts from
        assertEquals(0, new BigDecimal("18000").compareTo(price.subtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(price.discountTotal()));
        assertEquals(0, new BigDecimal("18000").compareTo(price.total()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PriceCalculationResult.PriceCalculationItemResult>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(promotionService).applyPromotions(captor.capture(), eq(new BigDecimal("18000")),
                isNull(), isNull(), eq(TENANT_ID), eq(PromotionChannel.WEB));
        assertEquals(0, new BigDecimal("9000").compareTo(captor.getValue().get(0).getUnitPrice()));
        assertEquals(10L, captor.getValue().get(0).getProductId());
    }

    @Test
    void price_returnsEngineDiscountAndPromotionCodes() {
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(discount("WEB10", "10% off", new BigDecimal("1200")));

        WebPricingService.CartPrice price = service.price(lines(BigDecimal.ONE), TENANT_ID, null);

        assertEquals(0, new BigDecimal("1200").compareTo(price.discountTotal()));
        assertEquals(0, new BigDecimal("10800").compareTo(price.total()));
        assertEquals("WEB10", price.joinedPromotionCodes());
    }

    @Test
    void price_clampsDiscountToSubtotal() {
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(discount("BIG", "huge", new BigDecimal("999999")));

        WebPricingService.CartPrice price = service.price(lines(BigDecimal.ONE), TENANT_ID, null);

        assertEquals(0, new BigDecimal("12000").compareTo(price.discountTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(price.total()));
    }

    @Test
    void price_resolvesLinkedArCustomerFromPhone() {
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.of(WebCustomer.builder()
                        .phone("998901234567").customerId(77L).tenantId(TENANT_ID).build()));
        when(promotionService.applyPromotions(anyList(), any(), eq(77L), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB))).thenReturn(noPromotions());

        service.price(lines(BigDecimal.ONE), TENANT_ID, "+998 90 123-45-67");

        verify(promotionService).applyPromotions(anyList(), any(), eq(77L), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB));
    }

    @Test
    void price_unknownPhoneStaysAnonymous() {
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998900000000"))
                .thenReturn(Optional.empty());
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB))).thenReturn(noPromotions());

        service.price(lines(BigDecimal.ONE), TENANT_ID, "+998900000000");

        verify(promotionService).applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB));
    }

    @Test
    void price_rejectsDraftItem() {
        liveItem.setStatus(WebCatalogStatus.DRAFT);
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(liveItem));

        assertThrows(ValidationException.class,
                () -> service.price(lines(BigDecimal.ONE), TENANT_ID, null));
    }

    @Test
    void price_rejectsUnknownItem() {
        when(catalogRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> service.price(lines(BigDecimal.ONE), TENANT_ID, null));
    }

    @Test
    void joinedPromotionCodes_truncatesAt500Chars() {
        String longCode = "X".repeat(300);
        WebPricingService.CartPrice price = new WebPricingService.CartPrice(List.of(),
                BigDecimal.TEN, BigDecimal.ONE,
                List.of(PriceCalculationResult.AppliedPromotion.builder().promotionCode(longCode).build(),
                        PriceCalculationResult.AppliedPromotion.builder().promotionCode(longCode).build()));

        assertEquals(500, price.joinedPromotionCodes().length());
    }

    @Test
    void joinedPromotionCodes_nullWhenNoPromotions() {
        WebPricingService.CartPrice price = new WebPricingService.CartPrice(List.of(),
                BigDecimal.TEN, BigDecimal.ZERO, List.of());

        assertNull(price.joinedPromotionCodes());
    }
}
