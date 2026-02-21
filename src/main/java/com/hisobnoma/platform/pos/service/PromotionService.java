package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final POSTransactionRepository transactionRepository;
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
        Long tenantId = securityContextHelper.getCurrentTenantId();

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
                return evaluateCategoryCondition(condition, items, tenantId);

            case BRAND:
                return evaluateBrandCondition(condition, items, tenantId);

            case CUSTOMER_GROUP:
                return evaluateCustomerGroupCondition(condition, customerId, tenantId);

            case FIRST_PURCHASE:
                return evaluateFirstPurchaseCondition(customerId, tenantId);

            default:
                return true;
        }
    }

    /**
     * Check if cart products belong to specified categories.
     */
    private boolean evaluateCategoryCondition(PromotionCondition condition,
                                               List<PriceCalculationResult.PriceCalculationItemResult> items,
                                               Long tenantId) {
        if (condition.getCategoryIds() == null || condition.getCategoryIds().isEmpty()) {
            return true;
        }

        Set<Long> requiredCategoryIds = parseIds(condition.getCategoryIds());
        if (requiredCategoryIds.isEmpty()) {
            return true;
        }

        // Get product IDs from cart
        Set<Long> productIds = items.stream()
                .map(PriceCalculationResult.PriceCalculationItemResult::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (productIds.isEmpty()) {
            return false;
        }

        // Fetch products and check their categories
        List<Product> products = productRepository.findAllById(productIds);

        Set<Long> cartCategoryIds = new HashSet<>();
        for (Product product : products) {
            if (product.getCategory() != null) {
                cartCategoryIds.add(product.getCategory().getId());
                // Also add parent categories for hierarchical matching
                if (product.getCategory().getParent() != null) {
                    cartCategoryIds.add(product.getCategory().getParent().getId());
                }
            }
        }

        // Check if any required category is present in cart
        for (Long requiredCategoryId : requiredCategoryIds) {
            if (cartCategoryIds.contains(requiredCategoryId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if cart products belong to specified brands.
     */
    private boolean evaluateBrandCondition(PromotionCondition condition,
                                            List<PriceCalculationResult.PriceCalculationItemResult> items,
                                            Long tenantId) {
        if (condition.getBrandIds() == null || condition.getBrandIds().isEmpty()) {
            return true;
        }

        Set<Long> requiredBrandIds = parseIds(condition.getBrandIds());
        if (requiredBrandIds.isEmpty()) {
            return true;
        }

        // Get product IDs from cart
        Set<Long> productIds = items.stream()
                .map(PriceCalculationResult.PriceCalculationItemResult::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (productIds.isEmpty()) {
            return false;
        }

        // Fetch products and check their brands
        List<Product> products = productRepository.findAllById(productIds);

        Set<Long> cartBrandIds = products.stream()
                .filter(p -> p.getBrand() != null)
                .map(p -> p.getBrand().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Check if any required brand is present in cart
        for (Long requiredBrandId : requiredBrandIds) {
            if (cartBrandIds.contains(requiredBrandId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if customer belongs to specified groups/types.
     * Uses the customerType field for customer classification.
     */
    private boolean evaluateCustomerGroupCondition(PromotionCondition condition, Long customerId, Long tenantId) {
        if (condition.getCustomerGroups() == null || condition.getCustomerGroups().isEmpty()) {
            return true;
        }

        if (customerId == null) {
            return false; // No customer to check
        }

        Set<String> requiredGroups = new HashSet<>(Arrays.asList(condition.getCustomerGroups().split(",")));
        requiredGroups = requiredGroups.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (requiredGroups.isEmpty()) {
            return true;
        }

        // Fetch customer and check type/group membership
        Optional<Customer> customerOpt = customerRepository.findByIdAndTenantId(customerId, tenantId);
        if (customerOpt.isEmpty()) {
            return false;
        }

        Customer customer = customerOpt.get();
        String customerType = customer.getCustomerType();

        if (customerType == null || customerType.isEmpty()) {
            return false;
        }

        // Check if customer's type matches any required group
        return requiredGroups.contains(customerType.trim().toUpperCase());
    }

    /**
     * Check if this is the customer's first purchase.
     */
    private boolean evaluateFirstPurchaseCondition(Long customerId, Long tenantId) {
        if (customerId == null) {
            return false; // Can't determine first purchase without customer
        }

        // Count completed transactions for this customer
        long completedTransactions = transactionRepository.countByCustomerIdAndTenantIdAndStatus(
                customerId, tenantId, TransactionStatus.COMPLETED);

        // If no completed transactions, this is the first purchase
        return completedTransactions == 0;
    }

    private PromotionApplicationDetail applyPromotion(Promotion promotion,
                                                       List<PriceCalculationResult.PriceCalculationItemResult> items,
                                                       BigDecimal orderTotal) {
        BigDecimal discountAmount;

        // Handle BOGO and BUNDLE with item-specific logic
        switch (promotion.getType()) {
            case BUY_X_GET_Y:
                discountAmount = calculateBogoDiscount(promotion, items);
                break;
            case BUNDLE:
                discountAmount = calculateBundleDiscount(promotion, items);
                break;
            default:
                discountAmount = calculatePromotionDiscount(promotion, orderTotal);
                break;
        }

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

    /**
     * Calculate BOGO (Buy X Get Y) discount.
     * Example: Buy 2 Get 1 Free (buyQuantity=2, getQuantity=1, getDiscountPercent=100)
     * Example: Buy 3 Get 1 50% Off (buyQuantity=3, getQuantity=1, getDiscountPercent=50)
     */
    private BigDecimal calculateBogoDiscount(Promotion promotion,
                                              List<PriceCalculationResult.PriceCalculationItemResult> items) {
        if (promotion.getBuyQuantity() == null || promotion.getGetQuantity() == null
                || promotion.getGetDiscountPercent() == null) {
            return BigDecimal.ZERO;
        }

        int buyQty = promotion.getBuyQuantity();
        int getQty = promotion.getGetQuantity();
        BigDecimal discountPercent = promotion.getGetDiscountPercent();

        // Get qualifying products from conditions
        Set<Long> qualifyingProductIds = getQualifyingProductIds(promotion);
        Set<Long> qualifyingCategoryIds = getQualifyingCategoryIds(promotion);

        // Collect all qualifying item units sorted by price (cheapest first for applying discount)
        List<BigDecimal> qualifyingPrices = new ArrayList<>();

        for (PriceCalculationResult.PriceCalculationItemResult item : items) {
            // Check if this item qualifies for the BOGO
            boolean qualifies = qualifyingProductIds.isEmpty() && qualifyingCategoryIds.isEmpty(); // All products if no restriction
            if (!qualifies && !qualifyingProductIds.isEmpty()) {
                qualifies = qualifyingProductIds.contains(item.getProductId());
            }
            // Note: Category check would require product repository lookup - simplified here

            if (qualifies) {
                // Add each unit's price to the list
                int unitCount = item.getQuantity().intValue();
                for (int i = 0; i < unitCount; i++) {
                    qualifyingPrices.add(item.getUnitPrice());
                }
            }
        }

        if (qualifyingPrices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Sort prices (cheapest first - discount applied to cheapest items)
        qualifyingPrices.sort(Comparator.naturalOrder());

        // Calculate how many complete "sets" we have (buyQty + getQty = 1 set)
        int setSize = buyQty + getQty;
        int totalUnits = qualifyingPrices.size();
        int completeSets = totalUnits / setSize;

        if (completeSets == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate discount: for each set, apply discount to the cheapest getQty items
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (int set = 0; set < completeSets; set++) {
            // In each set, the cheapest getQty items get the discount
            int startIdx = set * setSize;
            for (int i = 0; i < getQty && (startIdx + i) < qualifyingPrices.size(); i++) {
                BigDecimal itemPrice = qualifyingPrices.get(startIdx + i);
                BigDecimal itemDiscount = itemPrice.multiply(discountPercent)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                totalDiscount = totalDiscount.add(itemDiscount);
            }
        }

        return totalDiscount;
    }

    /**
     * Calculate Bundle discount.
     * All bundle products must be present in cart for discount to apply.
     */
    private BigDecimal calculateBundleDiscount(Promotion promotion,
                                                List<PriceCalculationResult.PriceCalculationItemResult> items) {
        // Get bundle product IDs from actions or conditions
        Set<Long> bundleProductIds = getBundleProductIds(promotion);

        if (bundleProductIds.isEmpty()) {
            // No specific bundle products defined - use discount value as flat discount
            if (promotion.getDiscountValue() != null) {
                return promotion.getDiscountValue();
            }
            return BigDecimal.ZERO;
        }

        // Check if all bundle products are in cart
        Set<Long> cartProductIds = items.stream()
                .map(PriceCalculationResult.PriceCalculationItemResult::getProductId)
                .collect(Collectors.toSet());

        if (!cartProductIds.containsAll(bundleProductIds)) {
            return BigDecimal.ZERO; // Not all bundle products present
        }

        // Calculate discount based on promotion settings
        if (promotion.getDiscountValue() != null) {
            // Fixed bundle discount
            return promotion.getDiscountValue();
        }

        // Alternative: calculate as percentage of bundle items total
        BigDecimal bundleTotal = items.stream()
                .filter(item -> bundleProductIds.contains(item.getProductId()))
                .map(PriceCalculationResult.PriceCalculationItemResult::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check actions for percentage discount
        for (PromotionAction action : promotion.getActions()) {
            if ("PERCENTAGE_OFF".equals(action.getActionType()) && action.getDiscountPercent() != null) {
                return bundleTotal.multiply(action.getDiscountPercent())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }
            if ("FIXED_AMOUNT_OFF".equals(action.getActionType()) && action.getDiscountAmount() != null) {
                return action.getDiscountAmount().min(bundleTotal);
            }
            if ("SET_PRICE".equals(action.getActionType()) && action.getSetPrice() != null) {
                // Bundle price is set - discount is difference
                return bundleTotal.subtract(action.getSetPrice()).max(BigDecimal.ZERO);
            }
        }

        return BigDecimal.ZERO;
    }

    private Set<Long> getQualifyingProductIds(Promotion promotion) {
        Set<Long> productIds = new HashSet<>();

        // From conditions
        for (PromotionCondition condition : promotion.getConditions()) {
            if (condition.getProductIds() != null) {
                productIds.addAll(parseIds(condition.getProductIds()));
            }
        }

        // From actions (target products)
        for (PromotionAction action : promotion.getActions()) {
            if (action.getTargetProductIds() != null) {
                productIds.addAll(parseIds(action.getTargetProductIds()));
            }
        }

        return productIds;
    }

    private Set<Long> getQualifyingCategoryIds(Promotion promotion) {
        Set<Long> categoryIds = new HashSet<>();

        // From conditions
        for (PromotionCondition condition : promotion.getConditions()) {
            if (condition.getCategoryIds() != null) {
                categoryIds.addAll(parseIds(condition.getCategoryIds()));
            }
        }

        // From actions
        for (PromotionAction action : promotion.getActions()) {
            if (action.getTargetCategoryIds() != null) {
                categoryIds.addAll(parseIds(action.getTargetCategoryIds()));
            }
        }

        return categoryIds;
    }

    private Set<Long> getBundleProductIds(Promotion promotion) {
        Set<Long> productIds = new HashSet<>();

        // Bundle products from conditions
        for (PromotionCondition condition : promotion.getConditions()) {
            if ("SPECIFIC_PRODUCTS".equals(condition.getConditionType().name()) && condition.getProductIds() != null) {
                productIds.addAll(parseIds(condition.getProductIds()));
            }
        }

        // From actions
        for (PromotionAction action : promotion.getActions()) {
            if (action.getTargetProductIds() != null) {
                productIds.addAll(parseIds(action.getTargetProductIds()));
            }
        }

        return productIds;
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
                // BOGO calculation is done in applyPromotion which has access to items
                // This method is only called when items are not available (coupon application)
                // Return a simplified estimate based on order total
                if (promotion.getBuyQuantity() != null && promotion.getGetQuantity() != null
                        && promotion.getGetDiscountPercent() != null) {
                    // Estimate: assume average item price and calculate discount
                    int totalQty = promotion.getBuyQuantity() + promotion.getGetQuantity();
                    int freeQty = promotion.getGetQuantity();
                    BigDecimal discountRatio = BigDecimal.valueOf(freeQty)
                            .multiply(promotion.getGetDiscountPercent())
                            .divide(BigDecimal.valueOf(totalQty * 100), 4, RoundingMode.HALF_UP);
                    return orderTotal.multiply(discountRatio).setScale(4, RoundingMode.HALF_UP);
                }
                break;

            case BUNDLE:
                // Bundle discount is calculated based on specific bundle price vs individual prices
                if (promotion.getDiscountValue() != null) {
                    return promotion.getDiscountValue().min(orderTotal);
                }
                break;

            case TIERED_DISCOUNT:
                // Tiered discount based on quantity thresholds - use actions
                return calculateTieredDiscount(promotion, orderTotal);

            case SPEND_X_GET_Y:
                // Spend $X get $Y off
                if (promotion.getMinOrderAmount() != null && orderTotal.compareTo(promotion.getMinOrderAmount()) >= 0) {
                    if (promotion.getDiscountValue() != null) {
                        return promotion.getDiscountValue().min(orderTotal);
                    }
                }
                break;

            case FREE_ITEM:
                // Free item value - would need product price
                return BigDecimal.ZERO;

            default:
                break;
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTieredDiscount(Promotion promotion, BigDecimal orderTotal) {
        // Look for tiered discount in actions
        if (promotion.getActions() != null && !promotion.getActions().isEmpty()) {
            // Find the highest threshold that's met
            PromotionAction bestAction = null;
            for (PromotionAction action : promotion.getActions()) {
                if ("SET_PRICE".equals(action.getActionType()) || "PERCENTAGE_OFF".equals(action.getActionType())) {
                    // Could use threshold from action if available
                    if (bestAction == null || action.getSortOrder() > bestAction.getSortOrder()) {
                        bestAction = action;
                    }
                }
            }
            if (bestAction != null && bestAction.getDiscountPercent() != null) {
                return orderTotal.multiply(bestAction.getDiscountPercent())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }
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
