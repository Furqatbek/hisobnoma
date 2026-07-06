package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.entity.DistributionPaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionOrderDto {
    private Long id;
    private String orderNumber;
    private DistributionOrderStatus status;
    private Long agentId;
    private Long visitId;
    private Long routeId;
    private Long customerId;
    private String customerName;
    private Long sourceLocationId;
    private Long priceListId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private Instant deliveredAt;
    private DistributionPaymentMethod paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private BigDecimal cashCollected;
    private BigDecimal creditAmount;
    private Integer paymentTermsDays;
    private LocalDate dueDate;
    private Long arInvoiceId;
    private String arInvoiceNumber;
    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    private String currency;
    private String notes;
    private String internalNotes;
    private String cancellationReason;
    private boolean stockSettled;
    private List<DistributionOrderLineDto> lines;
}
