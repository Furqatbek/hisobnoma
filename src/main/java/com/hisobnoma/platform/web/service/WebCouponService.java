package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.service.PromotionService;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Validates coupon codes for the online shop. Per-customer limits key off the
 * AR customer linked to the phone's web account; anonymous users (or unknown
 * phones) get only the coupon's global limits. All invalid reasons collapse
 * into one generic outcome so the endpoint can't be used as a coupon oracle.
 */
@Service
@RequiredArgsConstructor
public class WebCouponService {

    private final PromotionService promotionService;
    private final WebCustomerRepository webCustomerRepository;

    /**
     * @param valid    whether the coupon applies
     * @param discount discount amount (zero when invalid), clamped to the total
     */
    public record CouponOutcome(boolean valid, BigDecimal discount) {
        public static CouponOutcome invalid() {
            return new CouponOutcome(false, BigDecimal.ZERO);
        }
    }

    /**
     * @param orderTotal the cart total the coupon discounts (after automatic
     *                   promotion discounts, before delivery)
     */
    @Transactional
    public CouponOutcome validate(String couponCode, BigDecimal orderTotal, Long tenantId, String phone) {
        if (couponCode == null || couponCode.isBlank()) {
            return CouponOutcome.invalid();
        }
        Long customerId = resolveArCustomerId(tenantId, phone);
        PriceCalculationResult.CouponApplication application = promotionService.applyCoupon(
                couponCode.trim(), orderTotal, customerId, tenantId, PromotionChannel.WEB);
        if (!application.isValid() || application.getDiscountAmount() == null
                || application.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return CouponOutcome.invalid();
        }
        return new CouponOutcome(true, application.getDiscountAmount().min(orderTotal));
    }

    private Long resolveArCustomerId(Long tenantId, String phone) {
        String normalized = WebOrderPublicService.normalizePhone(phone);
        if (normalized.isEmpty()) {
            return null;
        }
        return webCustomerRepository.findByTenantIdAndPhone(tenantId, normalized)
                .map(WebCustomer::getCustomerId)
                .orElse(null);
    }
}
