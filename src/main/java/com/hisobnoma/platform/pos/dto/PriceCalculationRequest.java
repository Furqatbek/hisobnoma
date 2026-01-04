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
public class PriceCalculationRequest {

    /**
     * Items to calculate pricing for
     */
    private List<PriceCalculationItem> items;

    /**
     * Customer ID for customer-specific pricing
     */
    private Long customerId;

    /**
     * Location ID for location-specific pricing
     */
    private Long locationId;

    /**
     * Coupon code to apply
     */
    private String couponCode;

    /**
     * Whether to include promotion calculations
     */
    @Builder.Default
    private boolean applyPromotions = true;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceCalculationItem {
        private Long productId;
        private Long variantId;
        private BigDecimal quantity;
        private BigDecimal overridePrice; // Manual price override
    }
}
