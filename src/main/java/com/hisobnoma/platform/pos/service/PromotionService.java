package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.mapper.PromotionMapper;
import com.hisobnoma.platform.pos.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Promotion management (CRUD, lifecycle, conditions/actions) and the facade
 * for promotion application and coupon redemption — the actual logic lives
 * in {@link PromotionEngine} and {@link CouponRedemptionService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionConditionRepository conditionRepository;
    private final PromotionActionRepository actionRepository;
    private final CouponRepository couponRepository;
    private final PromotionMapper promotionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final PromotionEngine promotionEngine;
    private final CouponRedemptionService couponRedemptionService;

    // ==================== CRUD Operations ====================

    @Transactional(readOnly = true)
    public Page<PromotionDto> findAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return promotionRepository.findByTenantId(tenantId, pageable)
                .map(promotionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PromotionDto> search(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return promotionRepository.searchByTenantId(query, tenantId, pageable)
                .map(promotionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PromotionDto findById(Long id) {
        return promotionMapper.toDtoWithDetails(getPromotionById(id));
    }

    @Transactional(readOnly = true)
    public PromotionDto findByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return promotionRepository.findByCodeAndTenantId(code, tenantId)
                .map(promotionMapper::toDtoWithDetails)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + code));
    }

    @Transactional
    public PromotionDto create(CreatePromotionRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (promotionRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Promotion code already exists: " + request.getCode());
        }

        Promotion promotion = promotionMapper.toEntity(request);
        promotion.setTenantId(tenantId);

        promotion = promotionRepository.save(promotion);

        // Add conditions
        if (request.getConditions() != null) {
            for (CreatePromotionConditionRequest condReq : request.getConditions()) {
                PromotionCondition condition = PromotionCondition.builder()
                        .promotion(promotion)
                        .conditionType(condReq.getConditionType())
                        .operator(condReq.getOperator() != null ? condReq.getOperator() : "GTE")
                        .value(condReq.getValue())
                        .value2(condReq.getValue2())
                        .thresholdAmount(condReq.getThresholdAmount())
                        .productIds(condReq.getProductIds())
                        .categoryIds(condReq.getCategoryIds())
                        .brandIds(condReq.getBrandIds())
                        .customerGroups(condReq.getCustomerGroups())
                        .required(condReq.isRequired())
                        .notes(condReq.getNotes())
                        .build();
                promotion.addCondition(condition);
            }
        }

        // Add actions
        if (request.getActions() != null) {
            int sortOrder = 0;
            for (CreatePromotionActionRequest actReq : request.getActions()) {
                PromotionAction action = PromotionAction.builder()
                        .promotion(promotion)
                        .actionType(actReq.getActionType())
                        .discountPercent(actReq.getDiscountPercent())
                        .discountAmount(actReq.getDiscountAmount())
                        .setPrice(actReq.getSetPrice())
                        .maxDiscount(actReq.getMaxDiscount())
                        .freeProductId(actReq.getFreeProductId())
                        .freeQuantity(actReq.getFreeQuantity() != null ? actReq.getFreeQuantity() : 1)
                        .targetProductIds(actReq.getTargetProductIds())
                        .targetCategoryIds(actReq.getTargetCategoryIds())
                        .applyTo(actReq.getApplyTo())
                        .applyCount(actReq.getApplyCount())
                        .sortOrder(actReq.getSortOrder() != null ? actReq.getSortOrder() : sortOrder++)
                        .notes(actReq.getNotes())
                        .build();
                promotion.addAction(action);
            }
        }

        promotion = promotionRepository.save(promotion);
        log.info("Created promotion: {}", promotion.getCode());

        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public PromotionDto update(Long id, CreatePromotionRequest request) {
        Promotion promotion = getPromotionById(id);

        // Check code uniqueness if changed
        if (!promotion.getCode().equals(request.getCode())) {
            Long tenantId = securityContextHelper.getCurrentTenantId();
            if (promotionRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
                throw new BusinessException("Promotion code already exists: " + request.getCode());
            }
        }

        promotionMapper.updateEntity(request, promotion);

        // Update conditions
        conditionRepository.deleteByPromotionId(id);
        promotion.getConditions().clear();
        if (request.getConditions() != null) {
            for (CreatePromotionConditionRequest condReq : request.getConditions()) {
                PromotionCondition condition = PromotionCondition.builder()
                        .promotion(promotion)
                        .conditionType(condReq.getConditionType())
                        .operator(condReq.getOperator() != null ? condReq.getOperator() : "GTE")
                        .value(condReq.getValue())
                        .value2(condReq.getValue2())
                        .thresholdAmount(condReq.getThresholdAmount())
                        .productIds(condReq.getProductIds())
                        .categoryIds(condReq.getCategoryIds())
                        .brandIds(condReq.getBrandIds())
                        .customerGroups(condReq.getCustomerGroups())
                        .required(condReq.isRequired())
                        .notes(condReq.getNotes())
                        .build();
                promotion.addCondition(condition);
            }
        }

        // Update actions
        actionRepository.deleteByPromotionId(id);
        promotion.getActions().clear();
        if (request.getActions() != null) {
            int sortOrder = 0;
            for (CreatePromotionActionRequest actReq : request.getActions()) {
                PromotionAction action = PromotionAction.builder()
                        .promotion(promotion)
                        .actionType(actReq.getActionType())
                        .discountPercent(actReq.getDiscountPercent())
                        .discountAmount(actReq.getDiscountAmount())
                        .setPrice(actReq.getSetPrice())
                        .maxDiscount(actReq.getMaxDiscount())
                        .freeProductId(actReq.getFreeProductId())
                        .freeQuantity(actReq.getFreeQuantity() != null ? actReq.getFreeQuantity() : 1)
                        .targetProductIds(actReq.getTargetProductIds())
                        .targetCategoryIds(actReq.getTargetCategoryIds())
                        .applyTo(actReq.getApplyTo())
                        .applyCount(actReq.getApplyCount())
                        .sortOrder(actReq.getSortOrder() != null ? actReq.getSortOrder() : sortOrder++)
                        .notes(actReq.getNotes())
                        .build();
                promotion.addAction(action);
            }
        }

        promotion = promotionRepository.save(promotion);
        log.info("Updated promotion: {}", promotion.getCode());

        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public PromotionDto activate(Long id) {
        Promotion promotion = getPromotionById(id);
        promotion.setActive(true);
        promotion = promotionRepository.save(promotion);
        log.info("Activated promotion: {}", promotion.getCode());
        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public PromotionDto deactivate(Long id) {
        Promotion promotion = getPromotionById(id);
        promotion.setActive(false);
        promotion = promotionRepository.save(promotion);
        log.info("Deactivated promotion: {}", promotion.getCode());
        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public void delete(Long id) {
        Promotion promotion = getPromotionById(id);
        // Delete associated coupons first
        List<Coupon> coupons = couponRepository.findByPromotionId(id);
        for (Coupon coupon : coupons) {
            if (coupon.getCurrentUses() > 0) {
                throw new BusinessException("Cannot delete promotion with redeemed coupons");
            }
        }
        couponRepository.deleteAll(coupons);
        promotionRepository.delete(promotion);
        log.info("Deleted promotion: {}", promotion.getCode());
    }

    @Transactional(readOnly = true)
    public List<PromotionDto> findActivePromotions(Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        List<Promotion> promotions = promotionRepository.findActivePromotions(tenantId, today, locationId);
        return promotions.stream()
                .map(promotionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromotionDto addCondition(Long promotionId, CreatePromotionConditionRequest request) {
        Promotion promotion = getPromotionById(promotionId);
        PromotionCondition condition = PromotionCondition.builder()
                .promotion(promotion)
                .conditionType(request.getConditionType())
                .operator(request.getOperator() != null ? request.getOperator() : "GTE")
                .value(request.getValue())
                .value2(request.getValue2())
                .thresholdAmount(request.getThresholdAmount())
                .productIds(request.getProductIds())
                .categoryIds(request.getCategoryIds())
                .brandIds(request.getBrandIds())
                .customerGroups(request.getCustomerGroups())
                .required(request.isRequired())
                .notes(request.getNotes())
                .build();
        promotion.addCondition(condition);
        promotion = promotionRepository.save(promotion);
        log.info("Added condition to promotion: {}", promotion.getCode());
        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public PromotionDto removeCondition(Long promotionId, Long conditionId) {
        Promotion promotion = getPromotionById(promotionId);
        promotion.getConditions().removeIf(c -> c.getId().equals(conditionId));
        conditionRepository.deleteById(conditionId);
        promotion = promotionRepository.save(promotion);
        log.info("Removed condition {} from promotion: {}", conditionId, promotion.getCode());
        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public PromotionDto addAction(Long promotionId, CreatePromotionActionRequest request) {
        Promotion promotion = getPromotionById(promotionId);
        int sortOrder = promotion.getActions().size();
        PromotionAction action = PromotionAction.builder()
                .promotion(promotion)
                .actionType(request.getActionType())
                .discountPercent(request.getDiscountPercent())
                .discountAmount(request.getDiscountAmount())
                .setPrice(request.getSetPrice())
                .maxDiscount(request.getMaxDiscount())
                .freeProductId(request.getFreeProductId())
                .freeQuantity(request.getFreeQuantity() != null ? request.getFreeQuantity() : 1)
                .targetProductIds(request.getTargetProductIds())
                .targetCategoryIds(request.getTargetCategoryIds())
                .applyTo(request.getApplyTo())
                .applyCount(request.getApplyCount())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : sortOrder)
                .notes(request.getNotes())
                .build();
        promotion.addAction(action);
        promotion = promotionRepository.save(promotion);
        log.info("Added action to promotion: {}", promotion.getCode());
        return promotionMapper.toDtoWithDetails(promotion);
    }

    @Transactional
    public PromotionDto removeAction(Long promotionId, Long actionId) {
        Promotion promotion = getPromotionById(promotionId);
        promotion.getActions().removeIf(a -> a.getId().equals(actionId));
        actionRepository.deleteById(actionId);
        promotion = promotionRepository.save(promotion);
        log.info("Removed action {} from promotion: {}", actionId, promotion.getCode());
        return promotionMapper.toDtoWithDetails(promotion);
    }

    // ==================== Promotion Engine ====================

    // ==================== Promotion application (delegates to PromotionEngine) ====================

    /**
     * Apply automatic POS promotions to items (staff context).
     */
    @Transactional(readOnly = true)
    public PromotionApplicationResult applyPromotions(
            List<PriceCalculationResult.PriceCalculationItemResult> items,
            BigDecimal orderTotal,
            Long customerId,
            Long locationId) {
        return promotionEngine.applyPromotions(items, orderTotal, customerId, locationId,
                securityContextHelper.getCurrentTenantId(), PromotionChannel.POS);
    }

    /**
     * Apply automatic promotions for an explicit tenant and sales channel.
     * Used by the online shop where there is no authenticated staff context.
     */
    @Transactional(readOnly = true)
    public PromotionApplicationResult applyPromotions(
            List<PriceCalculationResult.PriceCalculationItemResult> items,
            BigDecimal orderTotal,
            Long customerId,
            Long locationId,
            Long tenantId,
            PromotionChannel channel) {
        return promotionEngine.applyPromotions(items, orderTotal, customerId, locationId, tenantId, channel);
    }

    // ==================== Coupons (delegates to CouponRedemptionService) ====================

    /**
     * Apply a coupon code (POS staff context).
     */
    @Transactional
    public PriceCalculationResult.CouponApplication applyCoupon(String couponCode, BigDecimal orderTotal, Long customerId) {
        return couponRedemptionService.applyCoupon(couponCode, orderTotal, customerId,
                securityContextHelper.getCurrentTenantId(), PromotionChannel.POS);
    }

    /**
     * Apply a coupon code for an explicit tenant and sales channel.
     */
    @Transactional
    public PriceCalculationResult.CouponApplication applyCoupon(String couponCode, BigDecimal orderTotal,
                                                                Long customerId, Long tenantId,
                                                                PromotionChannel channel) {
        return couponRedemptionService.applyCoupon(couponCode, orderTotal, customerId, tenantId, channel);
    }

    /**
     * Record coupon redemption (simplified version).
     */
    @Transactional
    public void recordCouponRedemption(String couponCode, Long customerId, Long orderId, BigDecimal discountApplied) {
        couponRedemptionService.recordCouponRedemption(couponCode, customerId, null, orderId,
                discountApplied, discountApplied);
    }

    /**
     * Record coupon redemption with full details.
     */
    @Transactional
    public void recordCouponRedemption(String couponCode, Long customerId, String customerEmail,
                                        Long transactionId, BigDecimal orderTotal, BigDecimal discountAmount) {
        couponRedemptionService.recordCouponRedemption(couponCode, customerId, customerEmail,
                transactionId, orderTotal, discountAmount);
    }

    /**
     * Record the redemption of a coupon attached to an online order.
     */
    @Transactional
    public void recordWebCouponRedemption(String couponCode, Long customerId, Long webOrderId,
                                          BigDecimal orderTotal, BigDecimal discountAmount,
                                          Long tenantId, Long userId) {
        couponRedemptionService.recordWebCouponRedemption(couponCode, customerId, webOrderId,
                orderTotal, discountAmount, tenantId, userId);
    }

    /**
     * Reverse the redemption when a confirmed online order is cancelled.
     */
    @Transactional
    public void reverseWebCouponRedemption(Long webOrderId, Long tenantId, Long userId, String reason) {
        couponRedemptionService.reverseWebCouponRedemption(webOrderId, tenantId, userId, reason);
    }

    // ==================== Private Helper Methods ====================

    private Promotion getPromotionById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return promotionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + id));
    }

    // ==================== Result Classes ====================

    @Getter
    @AllArgsConstructor
    public static class PromotionApplicationResult {
        private final List<PriceCalculationResult.PriceCalculationItemResult> updatedItems;
        private final List<PriceCalculationResult.AppliedPromotion> appliedPromotions;
        private final BigDecimal totalDiscount;
    }

    @AllArgsConstructor
    private static class PromotionApplicationDetail {
        final BigDecimal discountAmount;
        final List<PriceCalculationResult.PriceCalculationItemResult> updatedItems;
    }
}
