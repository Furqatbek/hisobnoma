package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.POStatus;
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
public class PurchaseOrderDto {

    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private POStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate receivedDate;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentTerms;
    private String shippingMethod;
    private String shippingAddress;
    private String notes;
    private String internalNotes;
    private String vendorReference;
    private Long approvedBy;
    private Instant approvedAt;
    private Long cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    private List<PurchaseOrderLineDto> lines;
    private boolean fullyReceived;
    private boolean partiallyReceived;
    private Instant createdAt;
    private Long createdBy;
}
