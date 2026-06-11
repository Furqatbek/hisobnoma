package com.hisobnoma.platform.web.dto;

import com.hisobnoma.platform.web.entity.WebOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Staff-facing view of an online order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebOrderDto {

    private Long id;
    private String orderNumber;
    private WebOrderStatus status;
    private String customerName;
    private String phone;
    private Long deliveryRegionId;
    private String deliveryRegionName;
    private Long deliveryVillageId;
    private String deliveryVillageName;
    private String customerNote;
    private BigDecimal deliveryFee;
    private BigDecimal discountTotal;
    private String appliedPromotions;
    private String couponCode;
    private BigDecimal couponDiscount;
    private BigDecimal pointsSpent;
    private BigDecimal totalAmount;
    private String currency;
    private Long customerId;
    private Long arInvoiceId;
    private String arInvoiceNumber;
    private String cancellationReason;
    private Instant createdAt;
    private List<WebOrderLineDto> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebOrderLineDto {
        private Long id;
        private Long productId;
        private String productName;
        private String unitName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
