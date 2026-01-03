package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APInvoiceLineDto {
    private Long id;
    private Long apInvoiceId;
    private Integer lineNumber;
    private Long productId;
    private String productSku;
    private String productName;
    private String description;
    private Long expenseAccountId;
    private String expenseAccountCode;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
    private String taxCode;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private Long purchaseOrderLineId;
    private Long receivingLineId;
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal varianceQuantity;
    private BigDecimal varianceAmount;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
