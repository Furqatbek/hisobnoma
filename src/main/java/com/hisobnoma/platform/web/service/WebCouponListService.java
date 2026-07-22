package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.repository.CouponRedemptionRepository;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.web.dto.PublicCouponDto;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebCouponListService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final WebCustomerRepository webCustomerRepository;

    @Transactional(readOnly = true)
    public List<PublicCouponDto> getAvailableCoupons(Long tenantId, Long webCustomerId) {
        Long arCustomerId = webCustomerRepository.findByIdAndTenantId(webCustomerId, tenantId)
                .map(WebCustomer::getCustomerId)
                .orElse(null);

        Long lookupCustomerId = arCustomerId != null ? arCustomerId : -1L;

        List<Coupon> coupons = couponRepository.findAvailableForWebCustomer(
                tenantId, lookupCustomerId, webCustomerId, LocalDate.now());

        return coupons.stream()
                .filter(c -> {
                    if (arCustomerId == null || c.getMaxUsesPerCustomer() == null) return true;
                    int used = redemptionRepository.countByCustomerAndCoupon(c.getId(), arCustomerId);
                    return used < c.getMaxUsesPerCustomer();
                })
                .map(this::toDto)
                .toList();
    }

    private PublicCouponDto toDto(Coupon c) {
        Promotion p = c.getPromotion();
        String discountType;
        if (p.getType() == PromotionType.PERCENTAGE_OFF) {
            discountType = "PERCENT";
        } else if (p.getType() == PromotionType.FIXED_AMOUNT_OFF) {
            discountType = "FIXED";
        } else {
            discountType = p.getType().name();
        }

        return PublicCouponDto.builder()
                .code(c.getCode())
                .description(c.getDescription() != null ? c.getDescription() : p.getDescription())
                .discountType(discountType)
                .discountValue(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderAmount())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .build();
    }
}
