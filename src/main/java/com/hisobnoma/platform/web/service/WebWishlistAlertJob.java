package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebWishlistItem;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebWishlistAlertJob {

    static final String SETTING_SMS_ALERTS = "wishlist.sms_alerts_enabled";
    static final String SETTING_MAX_SMS_PER_DAY = "wishlist.max_sms_per_day";

    private final WebWishlistItemRepository wishlistRepository;
    private final WebCatalogItemRepository catalogRepository;
    private final WebWishlistService wishlistService;
    private final WebPushService pushService;
    private final WebPromotionBadgeService badgeService;
    private final WebDeviceTokenRepository deviceTokenRepository;
    private final WebCustomerRepository webCustomerRepository;
    private final SmsService smsService;
    private final TenantSettingService tenantSettingService;

    /**
     * Per-customer daily SMS counters, keyed by "tenant:customer:day".
     * Single-instance only — acceptable for a best-effort cost cap; a
     * multi-instance deployment should move this to a shared store.
     */
    private final Map<String, Integer> smsDailyCount = new ConcurrentHashMap<>();

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void processAlerts() {
        evictStaleSmsCounters();
        List<Long> tenantIds = wishlistRepository.findDistinctTenantIds();
        for (Long tenantId : tenantIds) {
            try {
                processAlertsForTenant(tenantId);
            } catch (Exception e) {
                log.warn("Wishlist alert job failed for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    void processAlertsForTenant(Long tenantId) {
        List<Long> catalogItemIds = wishlistRepository.findDistinctCatalogItemIdsByTenantId(tenantId);
        int alertsSent = 0;

        for (Long catalogItemId : catalogItemIds) {
            WebCatalogItem catalogItem = catalogRepository.findByIdAndTenantId(catalogItemId, tenantId)
                    .orElse(null);
            if (catalogItem == null) continue;

            boolean currentlyAvailable = wishlistService.isAvailable(catalogItemId, tenantId);

            WebPromotionBadgeService.Badge badge = badgeService.badgeFor(
                    tenantId, catalogItem.getId(),
                    catalogItem.getProduct().getId(),
                    catalogItem.getEffectivePrice()).orElse(null);
            BigDecimal currentSalePrice = badge != null ? badge.salePrice() : null;

            List<WebWishlistItem> wishlistItems = wishlistRepository
                    .findByTenantIdAndCatalogItemId(tenantId, catalogItemId);

            String promotionLabel = badge != null ? badge.label() : null;

            for (WebWishlistItem wi : wishlistItems) {
                boolean discountAlert = shouldSendDiscountAlert(wi, currentSalePrice);
                boolean restockAlert = shouldSendRestockAlert(wi, currentlyAvailable);

                if (discountAlert || restockAlert) {
                    sendAlert(wi, catalogItem, discountAlert, restockAlert, currentSalePrice, promotionLabel);
                    alertsSent++;
                }

                wi.setLastKnownAvailable(currentlyAvailable);
                if (currentSalePrice != null) {
                    wi.setLastNotifiedSalePrice(currentSalePrice);
                }
                wishlistRepository.save(wi);
            }
        }

        if (alertsSent > 0) {
            log.info("Wishlist alerts: sent {} alerts for tenant {}", alertsSent, tenantId);
        }
    }

    boolean shouldSendDiscountAlert(WebWishlistItem wi, BigDecimal currentSalePrice) {
        if (currentSalePrice == null) return false;
        if (wi.getLastNotifiedSalePrice() == null) return true;
        return currentSalePrice.compareTo(wi.getLastNotifiedSalePrice()) != 0;
    }

    boolean shouldSendRestockAlert(WebWishlistItem wi, boolean currentlyAvailable) {
        return currentlyAvailable && !wi.isLastKnownAvailable();
    }

    private void sendAlert(WebWishlistItem wi, WebCatalogItem catalogItem,
                           boolean discountAlert, boolean restockAlert,
                           BigDecimal salePrice, String promotionLabel) {
        Long tenantId = wi.getTenantId();
        Long customerId = wi.getWebCustomerId();
        String productName = catalogItem.getEffectiveName();
        String priceSuffix = promotionLabel != null
                ? " (" + promotionLabel + ")"
                : "";

        String title;
        String body;

        if (discountAlert && restockAlert) {
            title = productName + " — нарх тушди ва яна сотувда!";
            body = productName + " энди " + formatPrice(salePrice) + " сўм" + priceSuffix + " ва яна мавжуд";
        } else if (discountAlert) {
            title = productName + " — нарх тушди!";
            body = productName + " энди " + formatPrice(salePrice) + " сўм" + priceSuffix;
        } else {
            title = productName + " — яна сотувда!";
            body = productName + " яна мавжуд";
        }

        Map<String, String> data = Map.of(
                "type", "WISHLIST_ALERT",
                "catalogItemId", wi.getCatalogItemId().toString());

        // The in-app notification is always recorded, but the customer only counts as REACHED by
        // push when FCM delivery is actually configured — while the FCM integration is a logged
        // no-op, a non-throwing send must not suppress the SMS fallback (otherwise customers with
        // a device token get neither a real push nor an SMS).
        boolean pushed = false;
        if (deviceTokenRepository.existsByTenantIdAndWebCustomerId(tenantId, customerId)) {
            try {
                pushService.sendToCustomer(tenantId, customerId, title, body, data);
                pushed = pushService.isDeliveryEnabled();
            } catch (Exception e) {
                log.warn("Wishlist push failed for customer {}: {}", customerId, e.getMessage());
            }
        }

        if (!pushed && isSmsAlertsEnabled(tenantId)) {
            String phone = webCustomerRepository.findByIdAndTenantId(customerId, tenantId)
                    .map(WebCustomer::getPhone).orElse(null);
            // No push token AND no phone → nothing to reach the customer with; don't consume the cap.
            if (phone != null && !phone.isBlank()) {
                int maxPerDay = getMaxSmsPerDay(tenantId);
                String dailyKey = tenantId + ":" + customerId + ":" +
                        Instant.now().truncatedTo(ChronoUnit.DAYS);
                // Atomic check-and-increment so concurrent sends can't exceed the cap
                java.util.concurrent.atomic.AtomicBoolean underCap = new java.util.concurrent.atomic.AtomicBoolean(false);
                smsDailyCount.compute(dailyKey, (k, v) -> {
                    int current = v == null ? 0 : v;
                    if (current < maxPerDay) {
                        underCap.set(true);
                        return current + 1;
                    }
                    return v;
                });
                if (underCap.get()) {
                    try {
                        smsService.sendSmsAsync("+" + phone, body);
                    } catch (Exception e) {
                        log.warn("Wishlist SMS failed for customer {}: {}", customerId, e.getMessage());
                    }
                }
            }
        }

        wi.setNotifiedAt(Instant.now());
    }

    /** Drops counters from previous days so the map cannot grow unboundedly. */
    private void evictStaleSmsCounters() {
        String today = Instant.now().truncatedTo(ChronoUnit.DAYS).toString();
        smsDailyCount.keySet().removeIf(key -> !key.endsWith(today));
    }

    private boolean isSmsAlertsEnabled(Long tenantId) {
        return "true".equalsIgnoreCase(
                tenantSettingService.getSettingValue(tenantId, SETTING_SMS_ALERTS));
    }

    private int getMaxSmsPerDay(Long tenantId) {
        String val = tenantSettingService.getSettingValue(tenantId, SETTING_MAX_SMS_PER_DAY);
        if (val == null) return 1;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 1; }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        return price.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
