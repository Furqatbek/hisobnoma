package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.service.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes the "sale price" badge shown on catalog cards by running the real
 * promotion engine on a single-unit anonymous cart of the product — so the
 * badge always matches what checkout would compute for that unit.
 *
 * Only PERCENTAGE_OFF promotions produce badges: they are the only type whose
 * per-unit discount is accurate regardless of what else ends up in the cart.
 * Results are cached for 60 seconds per (tenant, item, price); the catalog
 * list is the hottest public endpoint.
 */
@Service
@Slf4j
public class WebPromotionBadgeService {

    static final long CACHE_TTL_MILLIS = 60_000;
    private static final int CACHE_EVICTION_THRESHOLD = 10_000;

    private final PromotionService promotionService;
    private final Clock clock;

    @Autowired
    public WebPromotionBadgeService(PromotionService promotionService) {
        this(promotionService, Clock.systemUTC());
    }

    WebPromotionBadgeService(PromotionService promotionService, Clock clock) {
        this.promotionService = promotionService;
        this.clock = clock;
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public record Badge(BigDecimal salePrice, String label) {
    }

    /**
     * @return the discounted unit price and a "-NN%" label, or empty when no
     * WEB percentage promotion applies. Never throws — the catalog must
     * render even if the promotion engine fails.
     */
    public Optional<Badge> badgeFor(Long tenantId, Long catalogItemId, Long productId, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        long now = clock.millis();
        String key = tenantId + "|" + catalogItemId + "|" + price.stripTrailingZeros().toPlainString();
        CacheEntry cached = cache.get(key);
        if (cached != null && now - cached.createdMillis < CACHE_TTL_MILLIS) {
            return cached.badge;
        }

        Optional<Badge> badge = compute(tenantId, productId, price);
        evictStale(now);
        cache.put(key, new CacheEntry(now, badge));
        return badge;
    }

    private Optional<Badge> compute(Long tenantId, Long productId, BigDecimal price) {
        try {
            List<PriceCalculationResult.PriceCalculationItemResult> items = List.of(
                    PriceCalculationResult.PriceCalculationItemResult.builder()
                            .productId(productId)
                            .quantity(BigDecimal.ONE)
                            .basePrice(price)
                            .unitPrice(price)
                            .lineDiscount(BigDecimal.ZERO)
                            .lineTotal(price)
                            .appliedPromotionCodes(new ArrayList<>())
                            .build());

            PromotionService.PromotionApplicationResult result = promotionService.applyPromotions(
                    items, price, null, null, tenantId, PromotionChannel.WEB);

            BigDecimal discount = result.getAppliedPromotions().stream()
                    .filter(p -> PromotionType.PERCENTAGE_OFF.name().equals(p.getPromotionType()))
                    .map(PriceCalculationResult.AppliedPromotion::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .min(price);

            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                return Optional.empty();
            }
            BigDecimal salePrice = price.subtract(discount);
            int percent = discount.multiply(BigDecimal.valueOf(100))
                    .divide(price, 0, RoundingMode.HALF_UP)
                    .intValue();
            return Optional.of(new Badge(salePrice, "-" + percent + "%"));
        } catch (Exception e) {
            log.warn("Sale badge computation failed for product {} (tenant {}): {}",
                    productId, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    private void evictStale(long now) {
        if (cache.size() < CACHE_EVICTION_THRESHOLD) {
            return;
        }
        Iterator<Map.Entry<String, CacheEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().createdMillis >= CACHE_TTL_MILLIS) {
                it.remove();
            }
        }
    }

    private record CacheEntry(long createdMillis, Optional<Badge> badge) {
    }
}
