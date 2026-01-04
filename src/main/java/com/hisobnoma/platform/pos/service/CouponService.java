package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.ResourceNotFoundException;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.CouponRedemption;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import com.hisobnoma.platform.pos.mapper.CouponMapper;
import com.hisobnoma.platform.pos.repository.CouponRedemptionRepository;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing Coupons.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final PromotionRepository promotionRepository;
    private final CouponMapper couponMapper;
    private final SecurityContextHelper securityContextHelper;

    private static final String COUPON_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<CouponDto> findAllCoupons(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return couponRepository.findByTenantId(tenantId, pageable)
                .map(this::enrichCouponDto);
    }

    @Transactional(readOnly = true)
    public Page<CouponDto> findCouponsByPromotion(Long promotionId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        promotionRepository.findByIdAndTenantId(promotionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        return couponRepository.findByPromotionId(promotionId, pageable)
                .map(this::enrichCouponDto);
    }

    @Transactional(readOnly = true)
    public Page<CouponDto> findCouponsByStatus(CouponStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return couponRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(this::enrichCouponDto);
    }

    @Transactional(readOnly = true)
    public CouponDto findCouponById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Coupon coupon = couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional(readOnly = true)
    public CouponDto findCouponByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Coupon coupon = couponRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));
        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional
    public CouponDto createCoupon(CreateCouponRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Promotion promotion = promotionRepository.findByIdAndTenantId(request.getPromotionId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", request.getPromotionId()));

        String code = request.getCode();
        if (code == null || code.isEmpty()) {
            code = generateCouponCode();
        }

        // Check for duplicate code
        if (couponRepository.findByCodeAndTenantId(code, tenantId).isPresent()) {
            throw new IllegalArgumentException("Coupon with code '" + code + "' already exists");
        }

        Coupon coupon = couponMapper.toEntity(request);
        coupon.setTenantId(tenantId);
        coupon.setCode(code);
        coupon.setPromotion(promotion);
        coupon.setStatus(CouponStatus.ACTIVE);
        coupon.setCurrentUses(0);

        coupon = couponRepository.save(coupon);
        log.info("Created coupon: {} for promotion {} (ID: {})", code, promotion.getName(), coupon.getId());

        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional
    public List<CouponDto> generateCoupons(Long promotionId, int count, CreateCouponRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Promotion promotion = promotionRepository.findByIdAndTenantId(promotionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        List<CouponDto> generatedCoupons = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String code = generateCouponCode();
            while (couponRepository.findByCodeAndTenantId(code, tenantId).isPresent()) {
                code = generateCouponCode();
            }

            Coupon coupon = Coupon.builder()
                    .tenantId(tenantId)
                    .code(code)
                    .promotion(promotion)
                    .maxUses(request.getMaxUses())
                    .maxUsesPerCustomer(request.getMaxUsesPerCustomer())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .customerId(request.getCustomerId())
                    .customerEmail(request.getCustomerEmail())
                    .status(CouponStatus.ACTIVE)
                    .currentUses(0)
                    .build();

            coupon = couponRepository.save(coupon);
            generatedCoupons.add(enrichCouponDto(couponMapper.toDto(coupon)));
        }

        log.info("Generated {} coupons for promotion {}", count, promotion.getName());
        return generatedCoupons;
    }

    @Transactional
    public CouponDto updateCoupon(Long id, CreateCouponRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Coupon coupon = couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        // Check for duplicate code if changed
        if (request.getCode() != null && !request.getCode().equals(coupon.getCode())) {
            if (couponRepository.findByCodeAndTenantId(request.getCode(), tenantId).isPresent()) {
                throw new IllegalArgumentException("Coupon with code '" + request.getCode() + "' already exists");
            }
        }

        // Update promotion if changed
        if (request.getPromotionId() != null && !request.getPromotionId().equals(coupon.getPromotion().getId())) {
            Promotion promotion = promotionRepository.findByIdAndTenantId(request.getPromotionId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", request.getPromotionId()));
            coupon.setPromotion(promotion);
        }

        couponMapper.updateEntity(request, coupon);
        coupon = couponRepository.save(coupon);
        log.info("Updated coupon: {} (ID: {})", coupon.getCode(), id);

        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional
    public CouponDto activateCoupon(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Coupon coupon = couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        coupon.setStatus(CouponStatus.ACTIVE);
        coupon = couponRepository.save(coupon);
        log.info("Activated coupon: {} (ID: {})", coupon.getCode(), id);

        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional
    public CouponDto deactivateCoupon(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Coupon coupon = couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        coupon.setStatus(CouponStatus.INACTIVE);
        coupon = couponRepository.save(coupon);
        log.info("Deactivated coupon: {} (ID: {})", coupon.getCode(), id);

        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional
    public CouponDto cancelCoupon(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Coupon coupon = couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        coupon.setStatus(CouponStatus.CANCELLED);
        coupon = couponRepository.save(coupon);
        log.info("Cancelled coupon: {} (ID: {})", coupon.getCode(), id);

        return enrichCouponDto(couponMapper.toDto(coupon));
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Coupon coupon = couponRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        // Check if coupon has been used
        if (coupon.getCurrentUses() > 0) {
            throw new IllegalStateException("Cannot delete coupon that has been used. Consider deactivating instead.");
        }

        couponRepository.delete(coupon);
        log.info("Deleted coupon: {} (ID: {})", coupon.getCode(), id);
    }

    @Transactional(readOnly = true)
    public List<CouponRedemption> getCouponRedemptions(Long couponId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Coupon coupon = couponRepository.findByIdAndTenantId(couponId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", couponId));

        return couponRedemptionRepository.findByCouponId(couponId);
    }

    @Transactional
    public void updateExpiredCoupons() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();

        List<Coupon> expiredCoupons = couponRepository.findExpiredActiveCoupons(tenantId, today);
        for (Coupon coupon : expiredCoupons) {
            coupon.setStatus(CouponStatus.EXPIRED);
            couponRepository.save(coupon);
            log.info("Marked coupon {} as expired", coupon.getCode());
        }
    }

    @Transactional
    public void updateDepletedCoupons() {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<Coupon> depletedCoupons = couponRepository.findDepletedActiveCoupons(tenantId);
        for (Coupon coupon : depletedCoupons) {
            coupon.setStatus(CouponStatus.DEPLETED);
            couponRepository.save(coupon);
            log.info("Marked coupon {} as depleted", coupon.getCode());
        }
    }

    private String generateCouponCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(COUPON_CHARACTERS.charAt(random.nextInt(COUPON_CHARACTERS.length())));
        }
        return sb.toString();
    }

    private CouponDto enrichCouponDto(CouponDto dto) {
        // Add remaining uses calculation
        if (dto.getMaxUses() != null && dto.getCurrentUses() != null) {
            dto.setRemainingUses(dto.getMaxUses() - dto.getCurrentUses());
        }
        return dto;
    }

    private CouponDto enrichCouponDto(Coupon coupon) {
        CouponDto dto = couponMapper.toDto(coupon);
        return enrichCouponDto(dto);
    }
}
