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
public class ARInvoiceLineDto {
    private Long id;
    private Long arInvoiceId;
    private Integer lineNumber;
    private Long productId;
    private String productSku;
    private String productName;
    private String description;
    private Long revenueAccountId;
    private String revenueAccountCode;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
    private String taxCode;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private Long posLineId;
    private Long salesOrderLineId;
    private String notes;
    private BigDecimal profitMargin;
    private BigDecimal profitMarginPercent;
    private Instant createdAt;
    private Instant updatedAt;
}
