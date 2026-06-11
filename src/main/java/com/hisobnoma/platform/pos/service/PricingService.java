package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVariantRepository;
import com.hisobnoma.platform.pos.dto.PriceCalculationRequest;
import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Pricing Engine Service.
 * Calculates prices based on price lists, customer-specific pricing,
 * quantity tiers, and location-specific rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final CustomerPriceListRepository customerPriceListRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PromotionService promotionService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Calculate prices for a set of items.
     */
    @Transactional(readOnly = true)
    public PriceCalculationResult calculatePrices(PriceCalculationRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();

        List<PriceCalculationResult.PriceCalculationItemResult> itemResults = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        // Get applicable price lists for customer
        List<PriceList> applicablePriceLists = getApplicablePriceLists(
                tenantId, request.getCustomerId(), request.getLocationId(), today);

        for (PriceCalculationRequest.PriceCalculationItem item : request.getItems()) {
            PriceCalculationResult.PriceCalculationItemResult itemResult =
                    calculateItemPrice(item, applicablePriceLists, today);
            itemResults.add(itemResult);
            subtotal = subtotal.add(itemResult.getLineTotal());
        }

        // Apply promotions if requested
        List<PriceCalculationResult.AppliedPromotion> appliedPromotions = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        PriceCalculationResult.CouponApplication couponApplication = null;

        if (request.isApplyPromotions()) {
            // Apply automatic promotions
            PromotionService.PromotionApplicationResult promoResult =
                    promotionService.applyPromotions(itemResults, subtotal, request.getCustomerId(),
                            request.getLocationId());
            appliedPromotions = promoResult.getAppliedPromotions();
            totalDiscount = promoResult.getTotalDiscount();
            itemResults = promoResult.getUpdatedItems();

            // Apply coupon if provided
            if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
                couponApplication = promotionService.applyCoupon(
                        request.getCouponCode(), subtotal.subtract(totalDiscount),
                        request.getCustomerId());
                if (couponApplication.isValid()) {
                    totalDiscount = totalDiscount.add(couponApplication.getDiscountAmount());
                }
            }
        }

        BigDecimal grandTotal = subtotal.subtract(totalDiscount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        return PriceCalculationResult.builder()
                .items(itemResults)
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .taxAmount(BigDecimal.ZERO) // Tax calculation handled separately
                .grandTotal(grandTotal)
                .appliedPromotions(appliedPromotions)
                .couponApplication(couponApplication)
                .build();
    }

    /**
     * Get the best price for a single product.
     */
    @Transactional(readOnly = true)
    public BigDecimal getProductPrice(Long productId, Long variantId, BigDecimal quantity,
                                       Long customerId, Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId).orElse(null);
        if (product == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal basePrice = getBasePrice(product, variantId);
        List<PriceList> applicablePriceLists = getApplicablePriceLists(tenantId, customerId, locationId, today);

        return findBestPrice(productId, variantId, quantity, basePrice, applicablePriceLists, today);
    }

    private PriceCalculationResult.PriceCalculationItemResult calculateItemPrice(
            PriceCalculationRequest.PriceCalculationItem item,
            List<PriceList> applicablePriceLists,
            LocalDate date) {

        Long tenantId = securityContextHelper.getCurrentTenantId();
        Product product = productRepository.findByIdAndTenantId(item.getProductId(), tenantId).orElse(null);

        if (product == null) {
            return PriceCalculationResult.PriceCalculationItemResult.builder()
                    .productId(item.getProductId())
                    .variantId(item.getVariantId())
                    .quantity(item.getQuantity())
                    .basePrice(BigDecimal.ZERO)
                    .unitPrice(BigDecimal.ZERO)
                    .lineDiscount(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.ZERO)
                    .appliedPromotionCodes(new ArrayList<>())
                    .build();
        }

        BigDecimal basePrice = getBasePrice(product, item.getVariantId());
        BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;

        // Use override price if provided
        BigDecimal unitPrice;
        String priceListCode = null;

        if (item.getOverridePrice() != null) {
            unitPrice = item.getOverridePrice();
            priceListCode = "MANUAL";
        } else {
            // Find best price from price lists
            PriceResult priceResult = findBestPriceWithSource(
                    item.getProductId(), item.getVariantId(), quantity, basePrice, applicablePriceLists, date);
            unitPrice = priceResult.price;
            priceListCode = priceResult.priceListCode;
        }

        BigDecimal lineTotal = unitPrice.multiply(quantity).setScale(4, RoundingMode.HALF_UP);

        String variantName = null;
        if (item.getVariantId() != null) {
            ProductVariant variant = variantRepository.findById(item.getVariantId())
                    .filter(v -> v.getProduct().getId().equals(product.getId()))
                    .orElse(null);
            if (variant != null) {
                variantName = variant.getName();
            }
        }

        return PriceCalculationResult.PriceCalculationItemResult.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .productCode(product.getSku())
                .productName(product.getName())
                .variantName(variantName)
                .quantity(quantity)
                .basePrice(basePrice)
                .unitPrice(unitPrice)
                .lineDiscount(BigDecimal.ZERO)
                .lineTotal(lineTotal)
                .priceListCode(priceListCode)
                .appliedPromotionCodes(new ArrayList<>())
                .build();
    }

    private List<PriceList> getApplicablePriceLists(Long tenantId, Long customerId,
                                                     Long locationId, LocalDate date) {
        List<PriceList> priceLists = new ArrayList<>();

        // First, get customer-specific price lists
        if (customerId != null) {
            List<CustomerPriceList> customerPriceLists =
                    customerPriceListRepository.findEffectiveForCustomer(customerId, date);
            for (CustomerPriceList cpl : customerPriceLists) {
                if (cpl.getPriceList().isEffective(date)) {
                    priceLists.add(cpl.getPriceList());
                }
            }
        }

        // Then, get location-specific and general price lists
        if (locationId != null) {
            priceLists.addAll(priceListRepository.findEffectivePriceListsForLocation(tenantId, locationId, date));
        } else {
            priceLists.addAll(priceListRepository.findEffectivePriceLists(tenantId, date));
        }

        // Sort by priority (higher priority first)
        priceLists.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));

        return priceLists;
    }

    private BigDecimal getBasePrice(Product product, Long variantId) {
        if (variantId != null) {
            ProductVariant variant = variantRepository.findById(variantId)
                    .filter(v -> v.getProduct().getId().equals(product.getId()))
                    .orElse(null);
            if (variant != null) {
                // Use effective price which falls back to product price + priceDifference
                BigDecimal effectivePrice = variant.getEffectiveSellingPrice();
                if (effectivePrice != null) {
                    return effectivePrice;
                }
            }
        }
        return product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO;
    }

    private BigDecimal findBestPrice(Long productId, Long variantId, BigDecimal quantity,
                                      BigDecimal basePrice, List<PriceList> priceLists, LocalDate date) {
        return findBestPriceWithSource(productId, variantId, quantity, basePrice, priceLists, date).price;
    }

    private PriceResult findBestPriceWithSource(Long productId, Long variantId, BigDecimal quantity,
                                                 BigDecimal basePrice, List<PriceList> priceLists, LocalDate date) {
        BigDecimal bestPrice = basePrice;
        String bestPriceListCode = null;

        for (PriceList priceList : priceLists) {
            List<PriceListItem> items = priceListItemRepository.findEffectiveItems(
                    priceList.getId(), productId, variantId, quantity, date);

            if (!items.isEmpty()) {
                PriceListItem item = items.get(0); // First match (most specific)
                BigDecimal calculatedPrice = item.calculatePrice(basePrice);

                // Apply price list's default markup if no specific price/markup is set
                if (item.getPrice() == null && item.getMarkupPercent() == null
                        && priceList.getDefaultMarkupPercent() != null) {
                    BigDecimal markup = basePrice.multiply(priceList.getDefaultMarkupPercent())
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    calculatedPrice = basePrice.add(markup);
                }

                // Take the first (highest priority) price list that has a price
                if (calculatedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    return new PriceResult(calculatedPrice, priceList.getCode());
                }
            }
        }

        return new PriceResult(bestPrice, bestPriceListCode);
    }

    private static class PriceResult {
        final BigDecimal price;
        final String priceListCode;

        PriceResult(BigDecimal price, String priceListCode) {
            this.price = price;
            this.priceListCode = priceListCode;
        }
    }
}
