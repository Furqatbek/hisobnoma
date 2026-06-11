package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The promotion application engine: evaluates conditions and computes
 * discounts (percentage, fixed, BOGO, bundle, tiered) for a cart.
 *
 * Extracted from {@link PromotionService}, which keeps the promotion CRUD
 * and remains the facade callers use — the engine itself is channel- and
 * tenant-agnostic (everything is passed in, no security context).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionEngine {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final POSTransactionRepository transactionRepository;

    /**
     * Apply automatic promotions for an explicit tenant and sales channel.
     */
    @Transactional(readOnly = true)
    public PromotionService.PromotionApplicationResult applyPromotions(
            List<PriceCalculationResult.PriceCalculationItemResult> items,
            BigDecimal orderTotal,
            Long customerId,
            Long locationId,
            Long tenantId,
            PromotionChannel channel) {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Get active promotions for the channel (ALL applies everywhere)
        List<Promotion> promotions = promotionRepository.findActivePromotionsForChannels(
                tenantId, today, locationId,
                List.of(channel, PromotionChannel.ALL));

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
            // A non-stackable promotion means nothing else stacks with it
            if (hasNonStackableApplied) {
                break;
            }

            // Check conditions
            if (!evaluateConditions(promotion, updatedItems, orderTotal.subtract(totalDiscount), customerId, tenantId)) {
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

        return new PromotionService.PromotionApplicationResult(updatedItems, appliedPromotions, totalDiscount);
    }

    // ==================== Condition Evaluation ====================

    private boolean evaluateConditions(Promotion promotion, List<PriceCalculationResult.PriceCalculationItemResult> items,
                                        BigDecimal orderTotal, Long customerId, Long tenantId) {
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
            boolean met = evaluateCondition(condition, items, orderTotal, customerId, tenantId);
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
                                       BigDecimal orderTotal, Long customerId, Long tenantId) {
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

    // ==================== Discount Calculation ====================

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

    /**
     * Order-total-based discount for a single promotion. Also used by the
     * coupon flow, where no line items are available.
     */
    BigDecimal calculatePromotionDiscount(Promotion promotion, BigDecimal orderTotal) {
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

    @AllArgsConstructor
    private static class PromotionApplicationDetail {
        final BigDecimal discountAmount;
        final List<PriceCalculationResult.PriceCalculationItemResult> updatedItems;
    }
}
