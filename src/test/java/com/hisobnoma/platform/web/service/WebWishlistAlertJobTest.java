package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebWishlistItem;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class WebWishlistAlertJobTest {

    static final Long TENANT = 1L;
    static final Long CUSTOMER = 10L;
    static final Long ITEM_ID = 100L;

    @Mock private WebWishlistItemRepository wishlistRepository;
    @Mock private WebCatalogItemRepository catalogRepository;
    @Mock private WebWishlistService wishlistService;
    @Mock private WebPushService pushService;
    @Mock private WebPromotionBadgeService badgeService;
    @Mock private WebDeviceTokenRepository deviceTokenRepository;
    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private SmsService smsService;
    @Mock private TenantSettingService tenantSettingService;

    @InjectMocks
    private WebWishlistAlertJob job;

    private WebCustomer customerWithPhone(String phone) {
        WebCustomer c = WebCustomer.builder().tenantId(TENANT).phone(phone).build();
        c.setId(CUSTOMER);
        return c;
    }

    @Test
    void discountAlert_sentWhenSalePriceAppears() {
        WebWishlistItem wi = buildWishlistItem(true, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.of(new WebPromotionBadgeService.Badge(BigDecimal.valueOf(8500), "-15%")));
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(true);
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService).sendToCustomer(eq(TENANT), eq(CUSTOMER), contains("нарх тушди"),
                argThat(body -> body.contains("8500") && body.contains("(-15%)")), any());
        assertEquals(BigDecimal.valueOf(8500), wi.getLastNotifiedSalePrice());
    }

    @Test
    void discountAlert_notSentWhenSameSalePrice() {
        WebWishlistItem wi = buildWishlistItem(true, BigDecimal.valueOf(8500));
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.of(new WebPromotionBadgeService.Badge(BigDecimal.valueOf(8500), "-15%")));
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService, never()).sendToCustomer(any(), any(), any(), any(), any());
    }

    @Test
    void discountAlert_resentWhenPriceChanges() {
        WebWishlistItem wi = buildWishlistItem(true, BigDecimal.valueOf(9000));
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.of(new WebPromotionBadgeService.Badge(BigDecimal.valueOf(8500), "-15%")));
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(true);
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService).sendToCustomer(eq(TENANT), eq(CUSTOMER), contains("нарх тушди"), any(), any());
    }

    @Test
    void restockAlert_sentWhenItemComesBack() {
        WebWishlistItem wi = buildWishlistItem(false, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(true);
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService).sendToCustomer(eq(TENANT), eq(CUSTOMER), contains("яна сотувда"), any(), any());
        assertTrue(wi.isLastKnownAvailable());
    }

    @Test
    void restockAlert_notSentWhenAlreadyAvailable() {
        WebWishlistItem wi = buildWishlistItem(true, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService, never()).sendToCustomer(any(), any(), any(), any(), any());
    }

    @Test
    void smsFallback_usedWhenNoPushToken() {
        WebWishlistItem wi = buildWishlistItem(false, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(false);
        when(tenantSettingService.getSettingValue(TENANT, "wishlist.sms_alerts_enabled")).thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT, "wishlist.max_sms_per_day")).thenReturn("2");
        when(webCustomerRepository.findByIdAndTenantId(CUSTOMER, TENANT))
                .thenReturn(Optional.of(customerWithPhone("998901234567")));
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService, never()).sendToCustomer(any(), any(), any(), any(), any());
        // Real SMS is sent to the customer's phone (prefixed with +), not a placeholder log.
        // This scenario is a restock alert (no sale price), so the body says "яна мавжуд".
        verify(smsService).sendSmsAsync(eq("+998901234567"), contains("яна мавжуд"));
        assertNotNull(wi.getNotifiedAt());
    }

    @Test
    void smsFallback_firesEvenWithPushToken_whenFcmNotConfigured() {
        // The FCM stub "succeeds" without delivering; a device token must NOT suppress the SMS
        // while push delivery is unconfigured — otherwise the customer gets neither channel.
        WebWishlistItem wi = buildWishlistItem(false, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any())).thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(true);
        when(pushService.isDeliveryEnabled()).thenReturn(false);
        when(tenantSettingService.getSettingValue(TENANT, "wishlist.sms_alerts_enabled")).thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT, "wishlist.max_sms_per_day")).thenReturn("2");
        when(webCustomerRepository.findByIdAndTenantId(CUSTOMER, TENANT))
                .thenReturn(Optional.of(customerWithPhone("998901234567")));
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        // In-app notification still recorded via the push service…
        verify(pushService).sendToCustomer(eq(TENANT), eq(CUSTOMER), any(), any(), any());
        // …but the SMS fallback fires because no real push was delivered.
        verify(smsService).sendSmsAsync(eq("+998901234567"), contains("яна мавжуд"));
    }

    @Test
    void smsSuppressed_whenRealPushDelivered() {
        WebWishlistItem wi = buildWishlistItem(false, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any())).thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(true);
        when(pushService.isDeliveryEnabled()).thenReturn(true);
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService).sendToCustomer(eq(TENANT), eq(CUSTOMER), any(), any(), any());
        verify(smsService, never()).sendSmsAsync(any(), any());
    }

    @Test
    void smsFallback_noSmsWhenCustomerHasNoPhone() {
        WebWishlistItem wi = buildWishlistItem(false, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any())).thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(false);
        when(tenantSettingService.getSettingValue(TENANT, "wishlist.sms_alerts_enabled")).thenReturn("true");
        when(webCustomerRepository.findByIdAndTenantId(CUSTOMER, TENANT))
                .thenReturn(Optional.of(customerWithPhone(null)));
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(smsService, never()).sendSmsAsync(any(), any());
    }

    @Test
    void smsDisabled_noSmsSent() {
        WebWishlistItem wi = buildWishlistItem(false, null);
        WebCatalogItem catalogItem = buildCatalogItem();

        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(TENANT)).thenReturn(List.of(ITEM_ID));
        when(catalogRepository.findByIdAndTenantId(ITEM_ID, TENANT)).thenReturn(Optional.of(catalogItem));
        when(wishlistService.isAvailable(ITEM_ID, TENANT)).thenReturn(true);
        when(badgeService.badgeFor(eq(TENANT), eq(ITEM_ID), any(), any()))
                .thenReturn(Optional.empty());
        when(wishlistRepository.findByTenantIdAndCatalogItemId(TENANT, ITEM_ID)).thenReturn(List.of(wi));
        when(deviceTokenRepository.existsByTenantIdAndWebCustomerId(TENANT, CUSTOMER)).thenReturn(false);
        lenient().when(tenantSettingService.getSettingValue(TENANT, "wishlist.sms_alerts_enabled")).thenReturn("false");
        when(wishlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processAlertsForTenant(TENANT);

        verify(pushService, never()).sendToCustomer(any(), any(), any(), any(), any());
        verify(smsService, never()).sendSmsAsync(any(), any());
    }

    @Test
    void jobFailure_isolatedPerTenant() {
        when(wishlistRepository.findDistinctTenantIds()).thenReturn(List.of(1L, 2L));
        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(1L))
                .thenThrow(new RuntimeException("DB error"));
        when(wishlistRepository.findDistinctCatalogItemIdsByTenantId(2L))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> job.processAlerts());
    }

    @Test
    void shouldSendDiscountAlert_logic() {
        WebWishlistItem wi = buildWishlistItem(true, null);
        assertTrue(job.shouldSendDiscountAlert(wi, BigDecimal.valueOf(8500)));
        assertFalse(job.shouldSendDiscountAlert(wi, null));

        wi.setLastNotifiedSalePrice(BigDecimal.valueOf(8500));
        assertFalse(job.shouldSendDiscountAlert(wi, BigDecimal.valueOf(8500)));
        assertTrue(job.shouldSendDiscountAlert(wi, BigDecimal.valueOf(7000)));
    }

    @Test
    void shouldSendRestockAlert_logic() {
        WebWishlistItem wi = buildWishlistItem(false, null);
        assertTrue(job.shouldSendRestockAlert(wi, true));
        assertFalse(job.shouldSendRestockAlert(wi, false));

        wi.setLastKnownAvailable(true);
        assertFalse(job.shouldSendRestockAlert(wi, true));
    }

    private WebWishlistItem buildWishlistItem(boolean available, BigDecimal lastNotifiedSalePrice) {
        return WebWishlistItem.builder()
                .tenantId(TENANT)
                .webCustomerId(CUSTOMER)
                .catalogItemId(ITEM_ID)
                .lastKnownAvailable(available)
                .lastNotifiedSalePrice(lastNotifiedSalePrice)
                .build();
    }

    private WebCatalogItem buildCatalogItem() {
        Product product = Product.builder()
                .name("Test Product")
                .sellingPrice(BigDecimal.valueOf(10000))
                .active(true)
                .sellable(true)
                .trackInventory(false)
                .build();
        product.setId(50L);
        product.setImages(List.of());

        WebCatalogItem item = WebCatalogItem.builder()
                .product(product)
                .tenantId(TENANT)
                .status(WebCatalogStatus.LIVE)
                .sortOrder(0)
                .build();
        item.setId(ITEM_ID);
        return item;
    }
}
