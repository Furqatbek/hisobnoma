package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.mapper.PromotionMapper;
import com.hisobnoma.platform.pos.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Promotion Engine Service.
 * Handles promotion logic including percentage discounts, fixed discounts,
 * BOGO, bundle pricing, and coupon redemption.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionConditionRepository conditionRepository;
    private final PromotionActionRepository actionRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final PromotionMapper promotionMapper;
    private final SecurityContextHelper securityContextHelper;

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

    /**
     * Apply automatic promotions to items.
     */
    @Transactional(readOnly = true)
    public PromotionApplicationResult applyPromotions(
            List<PriceCalculationResult.PriceCalculationItemResult> items,
            BigDecimal orderTotal,
            Long customerId,
            Long locationId) {

        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Get active promotions
        List<Promotion> promotions = promotionRepository.findActivePromotions(tenantId, today, locationId);

        // Filter by time and stackability
        List<Promotion> applicablePromotions = promotions.stream()
                .filter(p -> p.isEffective(today, now))
                .filter(p -> !p.isRequiresCoupon())
                .collect(Collectors.toList());

        List<PriceCalculationResult.AppliedPromotion> appliedPromotions = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<PriceCalculationResult.PriceCalculationItemResult> updatedItems = new ArrayList<>(items);

        // Sort by priority and apply
        applicablePromotions.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));

        boolean hasNonStackableApplied = false;

        for (Promotion promotion : applicablePromotions) {
            // Check if we can stack this promotion
            if (hasNonStackableApplied && !promotion.isStackable()) {
                continue;
            }

            // Check conditions
            if (!evaluateConditions(promotion, updatedItems, orderTotal.subtract(totalDiscount), customerId)) {
                continue;
            }

            // Apply promotion
            PromotionApplicationDetail detail = applyPromotion(promotion, updatedItems, orderTotal.subtract(totalDiscount));

            if (detail.discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                appliedPromotions.add(PriceCalculationResult.AppliedPromotion.builder()
                        .promotionId(promotion.getId())
                        .promotionCode(promotion.getCode())
                        .promotionName(promotion.getName())
                        .promotionType(promotion.getType().name())
                        .discountAmount(detail.discountAmount)
                        .description(promotion.getDescription())
                        .build());

                totalDiscount = totalDiscount.add(detail.discountAmount);
                updatedItems = detail.updatedItems;

                if (!promotion.isStackable()) {
                    hasNonStackableApplied = true;
                }
            }
        }

        return new PromotionApplicationResult(updatedItems, appliedPromotions, totalDiscount);
    }

    /**
     * Apply a coupon code.
     */
    @Transactional
    public PriceCalculationResult.CouponApplication applyCoupon(String couponCode, BigDecimal orderTotal, Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();

        // Find valid coupon
        Optional<Coupon> couponOpt = customerId != null
                ? couponRepository.findValidCouponForCustomer(couponCode, tenantId, customerId, today)
                : couponRepository.findValidCoupon(couponCode, tenantId, today);

        if (couponOpt.isEmpty()) {
            return PriceCalculationResult.CouponApplication.builder()
                    .couponCode(couponCode)
                    .valid(false)
                    .message("Invalid or expired coupon code")
                    .errorMessage("Invalid or expired coupon code")
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }

        Coupon coupon = couponOpt.get();

        // Check per-customer usage
        if (customerId != null && coupon.getMaxUsesPerCustomer() != null) {
            int customerUsage = redemptionRepository.countByCustomerAndCoupon(coupon.getId(), customerId);
            if (customerUsage >= coupon.getMaxUsesPerCustomer()) {
                return PriceCalculationResult.CouponApplication.builder()
                        .couponCode(couponCode)
                        .valid(false)
                        .message("Coupon usage limit reached for this customer")
                        .errorMessage("Coupon usage limit reached for this customer")
                        .discountAmount(BigDecimal.ZERO)
                        .build();
            }
        }

        // Get the linked promotion
        Promotion promotion = coupon.getPromotion();
        if (!promotion.isActive()) {
            return PriceCalculationResult.CouponApplication.builder()
                    .couponCode(couponCode)
                    .valid(false)
                    .message("The promotion for this coupon is no longer active")
                    .errorMessage("The promotion for this coupon is no longer active")
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }

        // Check minimum order amount
        if (promotion.getMinOrderAmount() != null && orderTotal.compareTo(promotion.getMinOrderAmount()) < 0) {
            return PriceCalculationResult.CouponApplication.builder()
                    .couponCode(couponCode)
                    .valid(false)
                    .message("Minimum order amount of " + promotion.getMinOrderAmount() + " required")
                    .errorMessage("Minimum order amount of " + promotion.getMinOrderAmount() + " required")
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }

        // Calculate discount
        BigDecimal discountAmount = calculatePromotionDiscount(promotion, orderTotal);

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
     * Record coupon redemption (simplified version).
     */
    @Transactional
    public void recordCouponRedemption(String couponCode, Long customerId, Long orderId, BigDecimal discountApplied) {
        recordCouponRedemption(couponCode, customerId, null, orderId, discountApplied, discountApplied);
    }

    /**
     * Record coupon redemption.
     */
    @Transactional
    public void recordCouponRedemption(String couponCode, Long customerId, String customerEmail,
                                        Long transactionId, BigDecimal orderTotal, BigDecimal discountAmount) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        Coupon coupon = couponRepository.findByCodeAndTenantId(couponCode, tenantId)
                .orElseThrow(() -> new NotFoundException("Coupon not found: " + couponCode));

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

    // ==================== Private Helper Methods ====================

    private Promotion getPromotionById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return promotionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + id));
    }

    private boolean evaluateConditions(Promotion promotion, List<PriceCalculationResult.PriceCalculationItemResult> items,
                                        BigDecimal orderTotal, Long customerId) {
        List<PromotionCondition> conditions = promotion.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions = always applicable
        }

        // Check minimum order amount from promotion
        if (promotion.getMinOrderAmount() != null && orderTotal.compareTo(promotion.getMinOrderAmount()) < 0) {
            return false;
        }

        // Evaluate each condition
        for (PromotionCondition condition : conditions) {
            boolean met = evaluateCondition(condition, items, orderTotal, customerId);
            if (condition.isRequired() && !met) {
                return false; // Required condition not met
            }
            if (!condition.isRequired() && met) {
                return true; // Optional condition met
            }
        }

        return true;
    }

    private boolean evaluateCondition(PromotionCondition condition, List<PriceCalculationResult.PriceCalculationItemResult> items,
                                       BigDecimal orderTotal, Long customerId) {
        switch (condition.getConditionType()) {
            case MINIMUM_PURCHASE:
                return condition.getThresholdAmount() == null ||
                       orderTotal.compareTo(condition.getThresholdAmount()) >= 0;

            case MINIMUM_QUANTITY:
                BigDecimal totalQuantity = items.stream()
                        .map(PriceCalculationResult.PriceCalculationItemResult::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return condition.getThresholdAmount() == null ||
                       totalQuantity.compareTo(condition.getThresholdAmount()) >= 0;

            case SPECIFIC_PRODUCTS:
                if (condition.getProductIds() == null) return true;
                Set<Long> requiredProducts = parseIds(condition.getProductIds());
                Set<Long> cartProducts = items.stream()
                        .map(PriceCalculationResult.PriceCalculationItemResult::getProductId)
                        .collect(Collectors.toSet());
                return cartProducts.containsAll(requiredProducts);

            case CATEGORY:
                // Would need to check product categories - simplified for now
                return true;

            case BRAND:
                // Would need to check product brands - simplified for now
                return true;

            case CUSTOMER_GROUP:
                // Would need to check customer group membership - simplified for now
                return true;

            case FIRST_PURCHASE:
                // Would need to check if customer has previous orders - simplified for now
                return true;

            default:
                return true;
        }
    }

    private PromotionApplicationDetail applyPromotion(Promotion promotion,
                                                       List<PriceCalculationResult.PriceCalculationItemResult> items,
                                                       BigDecimal orderTotal) {
        BigDecimal discountAmount = calculatePromotionDiscount(promotion, orderTotal);

        // Apply max discount cap
        if (promotion.getMaxDiscountAmount() != null &&
            discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discountAmount = promotion.getMaxDiscountAmount();
        }

        // For line-item promotions, distribute discount
        List<PriceCalculationResult.PriceCalculationItemResult> updatedItems = new ArrayList<>();
        for (PriceCalculationResult.PriceCalculationItemResult item : items) {
            PriceCalculationResult.PriceCalculationItemResult updated =
                    PriceCalculationResult.PriceCalculationItemResult.builder()
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
                            .productCode(item.getProductCode())
                            .productName(item.getProductName())
                            .variantName(item.getVariantName())
                            .quantity(item.getQuantity())
                            .basePrice(item.getBasePrice())
                            .unitPrice(item.getUnitPrice())
                            .lineDiscount(item.getLineDiscount())
                            .lineTotal(item.getLineTotal())
                            .priceListCode(item.getPriceListCode())
                            .appliedPromotionCodes(new ArrayList<>(item.getAppliedPromotionCodes()))
                            .build();
            updated.getAppliedPromotionCodes().add(promotion.getCode());
            updatedItems.add(updated);
        }

        return new PromotionApplicationDetail(discountAmount, updatedItems);
    }

    private BigDecimal calculatePromotionDiscount(Promotion promotion, BigDecimal orderTotal) {
        switch (promotion.getType()) {
            case PERCENTAGE_OFF:
                if (promotion.getDiscountValue() != null) {
                    return orderTotal.multiply(promotion.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                }
                break;

            case FIXED_AMOUNT_OFF:
                if (promotion.getDiscountValue() != null) {
                    return promotion.getDiscountValue().min(orderTotal);
                }
                break;

            case BUY_X_GET_Y:
                // Would need more complex calculation based on cart items
                // Simplified: apply get discount percent to order
                if (promotion.getGetDiscountPercent() != null && promotion.getGetQuantity() != null) {
                    // This is a simplified calculation
                    return BigDecimal.ZERO; // Actual implementation would be more complex
                }
                break;

            default:
                break;
        }

        return BigDecimal.ZERO;
    }

    private Set<Long> parseIds(String ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
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
