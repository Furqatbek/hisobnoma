package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.CouponRedemption;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.repository.CouponRedemptionRepository;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Coupon validation and redemption: applying a code to an order total,
 * recording redemptions (POS and web) with row locks against concurrent
 * over-redemption, and reversing web redemptions on order cancellation.
 *
 * Extracted from {@link PromotionService}, which keeps the promotion CRUD
 * and remains the facade callers use.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponRedemptionService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionEngine promotionEngine;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Apply a coupon code for an explicit tenant and sales channel. A coupon
     * is only redeemable on the channel its promotion targets (ALL = both).
     */
    @Transactional
    public PriceCalculationResult.CouponApplication applyCoupon(String couponCode, BigDecimal orderTotal,
                                                                Long customerId, Long tenantId,
                                                                PromotionChannel channel) {
        LocalDate today = LocalDate.now();

        // Find valid coupon
        Optional<Coupon> couponOpt = customerId != null
                ? couponRepository.findValidCouponForCustomer(couponCode, tenantId, customerId, today)
                : couponRepository.findValidCoupon(couponCode, tenantId, today);

        if (couponOpt.isEmpty()) {
            return invalid(couponCode, "Invalid or expired coupon code");
        }

        Coupon coupon = couponOpt.get();

        // Check per-customer usage
        if (customerId != null && coupon.getMaxUsesPerCustomer() != null) {
            int customerUsage = redemptionRepository.countByCustomerAndCoupon(coupon.getId(), customerId);
            if (customerUsage >= coupon.getMaxUsesPerCustomer()) {
                return invalid(couponCode, "Coupon usage limit reached for this customer");
            }
        }

        // Get the linked promotion
        Promotion promotion = coupon.getPromotion();
        if (!promotion.isActive()) {
            return invalid(couponCode, "The promotion for this coupon is no longer active");
        }

        // Channel check: a WEB coupon must not redeem at the POS and vice versa
        if (promotion.getChannel() != channel && promotion.getChannel() != PromotionChannel.ALL) {
            return invalid(couponCode, "Coupon is not valid for this sales channel");
        }

        // Check minimum order amount
        if (promotion.getMinOrderAmount() != null && orderTotal.compareTo(promotion.getMinOrderAmount()) < 0) {
            return invalid(couponCode, "Minimum order amount of " + promotion.getMinOrderAmount() + " required");
        }

        // Calculate discount
        BigDecimal discountAmount = promotionEngine.calculatePromotionDiscount(promotion, orderTotal);

        // Build discount description
        String discountDescription = buildDiscountDescription(promotion, discountAmount);

        return PriceCalculationResult.CouponApplication.builder()
                .couponCode(couponCode)
                .valid(true)
                .message("Coupon applied successfully")
                .discountAmount(discountAmount)
                .promotionId(promotion.getId())
                .promotionName(promotion.getName())
                .discountDescription(discountDescription)
                .build();
    }

    private static PriceCalculationResult.CouponApplication invalid(String couponCode, String message) {
        return PriceCalculationResult.CouponApplication.builder()
                .couponCode(couponCode)
                .valid(false)
                .message(message)
                .errorMessage(message)
                .discountAmount(BigDecimal.ZERO)
                .build();
    }

    private String buildDiscountDescription(Promotion promotion, BigDecimal discountAmount) {
        switch (promotion.getType()) {
            case PERCENTAGE_OFF:
                return promotion.getDiscountValue() + "% off";
            case FIXED_AMOUNT_OFF:
                return "$" + discountAmount + " off";
            case BUY_X_GET_Y:
                return "Buy " + promotion.getBuyQuantity() + " Get " + promotion.getGetQuantity() + " free";
            default:
                return promotion.getName();
        }
    }

    /**
     * Record coupon redemption.
     * Locks the coupon row and re-validates usage limits to prevent concurrent over-redemption.
     */
    @Transactional
    public void recordCouponRedemption(String couponCode, Long customerId, String customerEmail,
                                        Long transactionId, BigDecimal orderTotal, BigDecimal discountAmount) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        // Lock the coupon row to prevent concurrent redemptions exceeding the limit
        Coupon coupon = couponRepository.findByCodeAndTenantIdForUpdate(couponCode, tenantId)
                .orElseThrow(() -> new NotFoundException("Coupon not found: " + couponCode));

        // Re-validate usage limits under the lock (applyCoupon() ran without a lock)
        if (!coupon.isValid(LocalDate.now())) {
            throw new BusinessException("Coupon is no longer valid");
        }
        if (customerId != null && coupon.getMaxUsesPerCustomer() != null) {
            int customerUsage = redemptionRepository.countByCustomerAndCoupon(coupon.getId(), customerId);
            if (customerUsage >= coupon.getMaxUsesPerCustomer()) {
                throw new BusinessException("Coupon usage limit reached for this customer");
            }
        }

        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .orderTotal(orderTotal)
                .discountAmount(discountAmount)
                .redeemedAt(Instant.now())
                .redeemedBy(userId)
                .build();

        redemptionRepository.save(redemption);
        coupon.recordUsage();
        couponRepository.save(coupon);

        // Increment promotion usage
        Promotion promotion = coupon.getPromotion();
        promotion.incrementUsage();
        promotionRepository.save(promotion);

        log.info("Recorded coupon redemption: {} for transaction {}", couponCode, transactionId);
    }

    /**
     * Records the redemption of a coupon attached to an online order when
     * staff confirm it. If the coupon got depleted between checkout and
     * confirmation, the order keeps its discount (staff accepted that price);
     * the redemption row notes the override instead of failing.
     * A coupon deleted in the meantime is skipped silently.
     */
    @Transactional
    public void recordWebCouponRedemption(String couponCode, Long customerId, Long webOrderId,
                                          BigDecimal orderTotal, BigDecimal discountAmount,
                                          Long tenantId, Long userId) {
        Optional<Coupon> couponOpt = couponRepository.findByCodeAndTenantIdForUpdate(couponCode, tenantId);
        if (couponOpt.isEmpty()) {
            log.warn("Coupon {} no longer exists; skipping redemption for web order {}", couponCode, webOrderId);
            return;
        }
        Coupon coupon = couponOpt.get();

        String notes = null;
        if (!coupon.isValid(LocalDate.now())) {
            notes = "Redeemed past coupon limits — discount was granted at checkout and kept on staff confirmation";
            log.warn("Coupon {} no longer valid at confirmation of web order {}; recording override", couponCode, webOrderId);
        }

        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .customerId(customerId)
                .webOrderId(webOrderId)
                .orderTotal(orderTotal)
                .discountAmount(discountAmount)
                .redeemedAt(Instant.now())
                .redeemedBy(userId)
                .notes(notes)
                .build();
        redemptionRepository.save(redemption);

        coupon.recordUsage();
        couponRepository.save(coupon);

        Promotion promotion = coupon.getPromotion();
        promotion.incrementUsage();
        promotionRepository.save(promotion);

        log.info("Recorded coupon redemption: {} for web order {}", couponCode, webOrderId);
    }

    /**
     * Reverses the redemption when a confirmed online order is cancelled:
     * marks the redemption rows reversed and releases coupon/promotion usage
     * so the customer can use the coupon again.
     */
    @Transactional
    public void reverseWebCouponRedemption(Long webOrderId, Long tenantId, Long userId, String reason) {
        List<CouponRedemption> redemptions = redemptionRepository.findByWebOrderIdAndReversedFalse(webOrderId);
        for (CouponRedemption redemption : redemptions) {
            redemption.setReversed(true);
            redemption.setReversedAt(Instant.now());
            redemption.setReversedBy(userId);
            redemption.setReversalReason(reason);
            redemptionRepository.save(redemption);

            Coupon coupon = couponRepository.findByCodeAndTenantIdForUpdate(
                    redemption.getCoupon().getCode(), tenantId).orElse(null);
            if (coupon != null) {
                coupon.releaseUsage();
                couponRepository.save(coupon);
                Promotion promotion = coupon.getPromotion();
                promotion.decrementUsage();
                promotionRepository.save(promotion);
            }
            log.info("Reversed coupon redemption {} for web order {}", redemption.getId(), webOrderId);
        }
    }
}
