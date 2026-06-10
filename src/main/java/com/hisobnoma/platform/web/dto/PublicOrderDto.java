package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Customer-facing view of an order (checkout response and status lookup).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOrderDto {

    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private Instant createdAt;
    private List<Line> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
