package com.hisobnoma.platform.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POSTransactionLineDto {
    private Long id;
    private Long transactionId;
    private Integer lineNumber;
    private Long productId;
    private Long variantId;
    private String productCode;
    private String productName;
    private String variantName;
    private String barcode;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private String discountReason;
    private String taxCode;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private boolean isReturn;
    private Long originalLineId;
    private String serialNumber;
    private String batchNumber;
    private Long locationId;
    private String notes;
}
