package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebWishlistItem;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final TenantSettingService tenantSettingService;

    private final Map<String, Integer> smsDailyCount = new HashMap<>();

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void processAlerts() {
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

        boolean pushed = false;
        if (deviceTokenRepository.existsByTenantIdAndWebCustomerId(tenantId, customerId)) {
            try {
                pushService.sendToCustomer(tenantId, customerId, title, body, data);
                pushed = true;
            } catch (Exception e) {
                log.warn("Wishlist push failed for customer {}: {}", customerId, e.getMessage());
            }
        }

        if (!pushed && isSmsAlertsEnabled(tenantId)) {
            int maxPerDay = getMaxSmsPerDay(tenantId);
            String dailyKey = tenantId + ":" + customerId + ":" +
                    Instant.now().truncatedTo(ChronoUnit.DAYS);
            int sent = smsDailyCount.getOrDefault(dailyKey, 0);
            if (sent < maxPerDay) {
                smsDailyCount.put(dailyKey, sent + 1);
                log.info("Wishlist SMS alert (placeholder): tenant={}, customer={}, product={}",
                        tenantId, customerId, productName);
            }
        }

        wi.setNotifiedAt(Instant.now());
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
