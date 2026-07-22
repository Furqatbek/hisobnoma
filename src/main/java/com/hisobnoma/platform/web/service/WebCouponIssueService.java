package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.service.CouponService;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.IssueCouponRequest;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Staff-issued personal coupons for online-shop customers — individually or in bulk to a segment.
 * Each issued coupon is single-use and BOUND to the web customer (web_customer_id), so it shows up
 * only in that customer's /me/coupons and is auditable, unlike campaign-minted codes which are
 * matched to recipients only positionally. Optionally notifies the customer by SMS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebCouponIssueService {

    private final WebCustomerRepository webCustomerRepository;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final SmsService smsService;
    private final SecurityContextHelper securityContextHelper;

    @Transactional
    public CouponDto issueToCustomer(Long webCustomerId, IssueCouponRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        WebCustomer customer = webCustomerRepository.findByIdAndTenantId(webCustomerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Web customer not found: " + webCustomerId));
        return issue(customer, request);
    }

    /**
     * Issues one personal coupon to EVERY customer in the segment (opted-out customers are already
     * excluded by the segment queries). Returns the number of coupons issued.
     */
    @Transactional
    public int issueToSegment(WebSegmentType segment, Integer param, IssueCouponRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<WebCustomer> customers = resolveSegment(tenantId, segment, param);
        int issued = 0;
        for (WebCustomer customer : customers) {
            issue(customer, request);
            issued++;
        }
        log.info("Issued {} personal coupons to segment {} (tenant {})", issued, segment, tenantId);
        return issued;
    }

    public List<WebCustomer> resolveSegment(Long tenantId, WebSegmentType segment, Integer param) {
        return switch (segment) {
            case ALL_CUSTOMERS -> webCustomerRepository.segmentAll(tenantId);
            case ORDERED_LAST_N_DAYS -> webCustomerRepository.segmentOrderedSince(tenantId, cutoff(param));
            case NO_ORDER_IN_N_DAYS -> webCustomerRepository.segmentNoOrderSince(tenantId, cutoff(param));
            case NEVER_ORDERED -> webCustomerRepository.segmentNeverOrdered(tenantId);
            case MIN_TOTAL_SPENT -> webCustomerRepository.segmentMinSpent(
                    tenantId, java.math.BigDecimal.valueOf(param != null ? param : 0));
        };
    }

    private Instant cutoff(Integer days) {
        if (days == null || days <= 0) {
            throw new ValidationException("Segment requires a positive day parameter");
        }
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private CouponDto issue(WebCustomer customer, IssueCouponRequest request) {
        int validityDays = request.getValidityDays() != null ? request.getValidityDays() : 30;
        CreateCouponRequest create = new CreateCouponRequest();
        create.setPromotionId(request.getPromotionId());
        create.setStartDate(LocalDate.now());
        create.setEndDate(LocalDate.now().plusDays(validityDays));
        create.setMaxUses(1);
        create.setMaxUsesPerCustomer(1);
        create.setDescription(request.getNote() != null ? request.getNote() : "Шахсий купон");
        // AR binding when the web account is linked to a finance customer (enforced at redemption).
        create.setCustomerId(customer.getCustomerId());

        CouponDto dto = couponService.generateCoupons(request.getPromotionId(), 1, create).get(0);

        // Web binding: the coupon appears only in THIS customer's /me/coupons.
        Coupon coupon = couponRepository.findByIdAndTenantId(dto.getId(), customer.getTenantId())
                .orElseThrow(() -> new NotFoundException("Coupon not found after mint: " + dto.getId()));
        coupon.setWebCustomerId(customer.getId());
        couponRepository.save(coupon);

        if (Boolean.TRUE.equals(request.getSendSms()) && customer.getPhone() != null) {
            try {
                smsService.sendSmsAsync("+" + customer.getPhone(),
                        "Сизга шахсий купон: " + dto.getCode());
            } catch (Exception e) {
                log.warn("Coupon SMS failed for customer {}: {}", customer.getId(), e.getMessage());
            }
        }
        return dto;
    }
}
