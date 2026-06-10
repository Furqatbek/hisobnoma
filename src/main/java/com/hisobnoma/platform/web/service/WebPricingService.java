package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.pos.dto.PriceCalculationResult;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.service.PromotionService;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.PublicCartPriceDto;
import com.hisobnoma.platform.web.entity.WebCatalogItem;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Prices a web cart through the shared promotion engine (channel = WEB).
 * Catalog price overrides win over everything: the catalog item's effective
 * price is the base the engine discounts from. Used by both the public
 * cart-price preview and checkout so the numbers always match.
 */
@Service
@RequiredArgsConstructor
public class WebPricingService {

    private final WebCatalogItemRepository catalogRepository;
    private final WebCustomerRepository webCustomerRepository;
    private final PromotionService promotionService;

    /**
     * One resolved, priced cart line (before promotions).
     */
    public record PricedLine(Long catalogItemId, Long productId, String productName,
                             BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    /**
     * Outcome of pricing a cart: resolved lines plus promotion totals.
     */
    public record CartPrice(List<PricedLine> lines, BigDecimal subtotal, BigDecimal discountTotal,
                            List<PriceCalculationResult.AppliedPromotion> appliedPromotions) {

        public BigDecimal total() {
            return subtotal.subtract(discountTotal).max(BigDecimal.ZERO);
        }

        public String joinedPromotionCodes() {
            if (appliedPromotions.isEmpty()) {
                return null;
            }
            String joined = String.join(",", appliedPromotions.stream()
                    .map(PriceCalculationResult.AppliedPromotion::getPromotionCode)
                    .toList());
            return joined.length() > 500 ? joined.substring(0, 500) : joined;
        }
    }

    /**
     * Resolves catalog items and prices the cart. Lines referencing items
     * that are not LIVE/active/sellable fail with a validation error —
     * identical rules to checkout.
     */
    @Transactional(readOnly = true)
    public CartPrice price(List<CheckoutRequest.CheckoutLine> lines, Long tenantId, String phone) {
        List<PricedLine> resolved = new ArrayList<>();
        for (CheckoutRequest.CheckoutLine line : lines) {
            WebCatalogItem item = catalogRepository
                    .findByIdAndTenantId(line.getCatalogItemId(), tenantId)
                    .filter(i -> i.getStatus() == WebCatalogStatus.LIVE)
                    .filter(i -> i.getProduct().isActive() && i.getProduct().isSellable())
                    .orElseThrow(() -> new ValidationException(
                            "Product is not available: " + line.getCatalogItemId()));
            BigDecimal unitPrice = item.getEffectivePrice();
            resolved.add(new PricedLine(item.getId(), item.getProduct().getId(),
                    item.getEffectiveName(), line.getQuantity(), unitPrice,
                    unitPrice.multiply(line.getQuantity())));
        }
        return priceResolved(resolved, tenantId, phone);
    }

    /**
     * Prices already-resolved lines (checkout path, which resolves items itself).
     */
    @Transactional(readOnly = true)
    public CartPrice priceResolved(List<PricedLine> resolved, Long tenantId, String phone) {
        BigDecimal subtotal = resolved.stream()
                .map(PricedLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PriceCalculationResult.PriceCalculationItemResult> engineItems = resolved.stream()
                .map(l -> PriceCalculationResult.PriceCalculationItemResult.builder()
                        .productId(l.productId())
                        .productName(l.productName())
                        .quantity(l.quantity())
                        .basePrice(l.unitPrice())
                        .unitPrice(l.unitPrice())
                        .lineDiscount(BigDecimal.ZERO)
                        .lineTotal(l.lineTotal())
                        .appliedPromotionCodes(new ArrayList<>())
                        .build())
                .toList();

        PromotionService.PromotionApplicationResult result = promotionService.applyPromotions(
                engineItems, subtotal, resolveArCustomerId(tenantId, phone), null,
                tenantId, PromotionChannel.WEB);

        // The engine guarantees per-promotion discounts; clamp the sum defensively
        BigDecimal discount = result.getTotalDiscount().min(subtotal).max(BigDecimal.ZERO);
        return new CartPrice(resolved, subtotal, discount, result.getAppliedPromotions());
    }

    /**
     * Promotion conditions about the customer (group, first purchase) key off
     * the AR customer that staff linked to this phone's web account, if any.
     */
    private Long resolveArCustomerId(Long tenantId, String phone) {
        String normalized = WebOrderPublicService.normalizePhone(phone);
        if (normalized.isEmpty()) {
            return null;
        }
        return webCustomerRepository.findByTenantIdAndPhone(tenantId, normalized)
                .map(com.hisobnoma.platform.web.entity.WebCustomer::getCustomerId)
                .orElse(null);
    }
}
