package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.service.PromotionService;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebCouponServiceTest {

    @Mock private PromotionService promotionService;
    @Mock private WebCustomerRepository webCustomerRepository;

    @InjectMocks
    private WebCouponService service;

    private static final Long TENANT_ID = 1L;

    private PriceCalculationResult.CouponApplication application(boolean valid, BigDecimal discount) {
        return PriceCalculationResult.CouponApplication.builder()
                .couponCode("WELCOME")
                .valid(valid)
                .discountAmount(discount)
                .build();
    }

    @Test
    void validate_validCouponReturnsDiscount() {
        when(promotionService.applyCoupon(eq("WELCOME"), eq(new BigDecimal("36000")),
                isNull(), eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(application(true, new BigDecimal("5000")));

        WebCouponService.CouponOutcome outcome =
                service.validate("WELCOME", new BigDecimal("36000"), TENANT_ID, null);

        assertTrue(outcome.valid());
        assertEquals(0, new BigDecimal("5000").compareTo(outcome.discount()));
    }

    @Test
    void validate_trimsCodeAndClampsDiscountToTotal() {
        when(promotionService.applyCoupon(eq("BIG"), any(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(application(true, new BigDecimal("99999")));

        WebCouponService.CouponOutcome outcome =
                service.validate("  BIG  ", new BigDecimal("10000"), TENANT_ID, null);

        assertTrue(outcome.valid());
        assertEquals(0, new BigDecimal("10000").compareTo(outcome.discount()));
    }

    @Test
    void validate_invalidCouponCollapsesToGenericOutcome() {
        when(promotionService.applyCoupon(any(), any(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(application(false, BigDecimal.ZERO));

        WebCouponService.CouponOutcome outcome =
                service.validate("EXPIRED", new BigDecimal("10000"), TENANT_ID, null);

        assertFalse(outcome.valid());
        assertEquals(0, BigDecimal.ZERO.compareTo(outcome.discount()));
    }

    @Test
    void validate_zeroDiscountCountsAsInvalid() {
        when(promotionService.applyCoupon(any(), any(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(application(true, BigDecimal.ZERO));

        assertFalse(service.validate("FREE0", new BigDecimal("10000"), TENANT_ID, null).valid());
    }

    @Test
    void validate_blankCodeIsInvalidWithoutEngineCall() {
        assertFalse(service.validate("  ", BigDecimal.TEN, TENANT_ID, null).valid());
        assertFalse(service.validate(null, BigDecimal.TEN, TENANT_ID, null).valid());
        verifyNoInteractions(promotionService);
    }

    @Test
    void validate_resolvesLinkedArCustomerForPerCustomerLimits() {
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.of(WebCustomer.builder()
                        .phone("998901234567").customerId(77L).tenantId(TENANT_ID).build()));
        when(promotionService.applyCoupon(eq("WELCOME"), any(), eq(77L),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(application(true, new BigDecimal("5000")));

        WebCouponService.CouponOutcome outcome =
                service.validate("WELCOME", new BigDecimal("36000"), TENANT_ID, "+998 90 123-45-67");

        assertTrue(outcome.valid());
        verify(promotionService).applyCoupon(eq("WELCOME"), any(), eq(77L),
                eq(TENANT_ID), eq(PromotionChannel.WEB));
    }

    @Test
    void validate_unknownPhoneGetsOnlyGlobalLimits() {
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998900000000"))
                .thenReturn(Optional.empty());
        when(promotionService.applyCoupon(eq("WELCOME"), any(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB)))
                .thenReturn(application(true, new BigDecimal("5000")));

        service.validate("WELCOME", new BigDecimal("36000"), TENANT_ID, "+998900000000");

        verify(promotionService).applyCoupon(eq("WELCOME"), any(), isNull(),
                eq(TENANT_ID), eq(PromotionChannel.WEB));
    }
}
