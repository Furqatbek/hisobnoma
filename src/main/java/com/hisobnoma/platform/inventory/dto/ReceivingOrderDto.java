package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.ReceivingStatus;
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
public class ReceivingOrderDto {

    private Long id;
    private String receivingNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private ReceivingStatus status;
    private LocalDate receivingDate;
    private String vendorDeliveryNote;
    private String vendorInvoiceNumber;
    private LocalDate vendorInvoiceDate;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private String internalNotes;
    private Long receivedBy;
    private Instant receivedAt;
    private Long approvedBy;
    private Instant approvedAt;
    private Long cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    private boolean stockUpdated;
    private boolean apInvoiceCreated;
    private Long apInvoiceId;
    private List<ReceivingLineDto> lines;
    private boolean hasVariances;
    private Instant createdAt;
    private Long createdBy;
}
