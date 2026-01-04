package com.hisobnoma.platform.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResult {

    private List<PriceCalculationItemResult> items;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private List<AppliedPromotion> appliedPromotions;
    private CouponApplication couponApplication;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceCalculationItemResult {
        private Long productId;
        private Long variantId;
        private String productCode;
        private String productName;
        private String variantName;
        private BigDecimal quantity;
        private BigDecimal basePrice;
        private BigDecimal unitPrice;
        private BigDecimal lineDiscount;
        private BigDecimal lineTotal;
        private String priceListCode;
        private List<String> appliedPromotionCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedPromotion {
        private Long promotionId;
        private String promotionCode;
        private String promotionName;
        private String promotionType;
        private BigDecimal discountAmount;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponApplication {
        private String couponCode;
        private boolean valid;
        private String message;
        private BigDecimal discountAmount;
        private Long promotionId;
        private String promotionName;
        private String discountDescription;
        private String errorMessage;
    }
}
