package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Server-computed cart pricing shown to the customer before checkout.
 * Exposes only promotion names — never conditions or usage counters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCartPriceDto {

    private List<Line> lines;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal total;
    private String currency;
    private List<String> appliedPromotions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private Long catalogItemId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
