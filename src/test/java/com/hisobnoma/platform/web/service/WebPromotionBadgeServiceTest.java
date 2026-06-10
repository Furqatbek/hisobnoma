package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPromotionBadgeServiceTest {

    @Mock
    private PromotionService promotionService;

    private final AtomicLong nowMillis = new AtomicLong(1_000_000L);

    private WebPromotionBadgeService service;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return Instant.ofEpochMilli(nowMillis.get()); }
        };
        service = new WebPromotionBadgeService(promotionService, clock);
    }

    private PromotionService.PromotionApplicationResult result(String type, BigDecimal discount) {
        return new PromotionService.PromotionApplicationResult(List.of(),
                List.of(PriceCalculationResult.AppliedPromotion.builder()
                        .promotionCode("P").promotionName("Promo")
                        .promotionType(type)
                        .discountAmount(discount)
                        .build()),
                discount);
    }

    @Test
    void badgeFor_percentagePromotionProducesSalePriceAndLabel() {
        when(promotionService.applyPromotions(anyList(), eq(new BigDecimal("12000")),
                isNull(), isNull(), eq(1L), eq(PromotionChannel.WEB)))
                .thenReturn(result("PERCENTAGE_OFF", new BigDecimal("1800")));

        Optional<WebPromotionBadgeService.Badge> badge =
                service.badgeFor(1L, 100L, 10L, new BigDecimal("12000"));

        assertTrue(badge.isPresent());
        assertEquals(0, new BigDecimal("10200").compareTo(badge.get().salePrice()));
        assertEquals("-15%", badge.get().label());
    }

    @Test
    void badgeFor_nonPercentagePromotionsProduceNoBadge() {
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB)))
                .thenReturn(result("FIXED_AMOUNT_OFF", new BigDecimal("5000")));

        assertTrue(service.badgeFor(1L, 100L, 10L, new BigDecimal("12000")).isEmpty());
    }

    @Test
    void badgeFor_noPromotionsMeansNoBadge() {
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB)))
                .thenReturn(new PromotionService.PromotionApplicationResult(
                        List.of(), List.of(), BigDecimal.ZERO));

        assertTrue(service.badgeFor(1L, 100L, 10L, new BigDecimal("12000")).isEmpty());
    }

    @Test
    void badgeFor_engineFailureMeansNoBadgeNotException() {
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB)))
                .thenThrow(new RuntimeException("engine down"));

        assertTrue(service.badgeFor(1L, 100L, 10L, new BigDecimal("12000")).isEmpty());
    }

    @Test
    void badgeFor_zeroOrNullPriceMeansNoBadge() {
        assertTrue(service.badgeFor(1L, 100L, 10L, BigDecimal.ZERO).isEmpty());
        assertTrue(service.badgeFor(1L, 100L, 10L, null).isEmpty());
        verifyNoInteractions(promotionService);
    }

    @Test
    void badgeFor_cachesResultWithinTtl() {
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB)))
                .thenReturn(result("PERCENTAGE_OFF", new BigDecimal("1200")));

        service.badgeFor(1L, 100L, 10L, new BigDecimal("12000"));
        service.badgeFor(1L, 100L, 10L, new BigDecimal("12000"));

        verify(promotionService, times(1)).applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB));
    }

    @Test
    void badgeFor_recomputesAfterTtlExpires() {
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB)))
                .thenReturn(result("PERCENTAGE_OFF", new BigDecimal("1200")));

        service.badgeFor(1L, 100L, 10L, new BigDecimal("12000"));
        nowMillis.addAndGet(WebPromotionBadgeService.CACHE_TTL_MILLIS + 1);
        service.badgeFor(1L, 100L, 10L, new BigDecimal("12000"));

        verify(promotionService, times(2)).applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB));
    }

    @Test
    void badgeFor_priceChangeBypassesCache() {
        when(promotionService.applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB)))
                .thenReturn(result("PERCENTAGE_OFF", new BigDecimal("1200")));

        service.badgeFor(1L, 100L, 10L, new BigDecimal("12000"));
        service.badgeFor(1L, 100L, 10L, new BigDecimal("15000"));

        verify(promotionService, times(2)).applyPromotions(anyList(), any(), isNull(), isNull(),
                eq(1L), eq(PromotionChannel.WEB));
    }
}
