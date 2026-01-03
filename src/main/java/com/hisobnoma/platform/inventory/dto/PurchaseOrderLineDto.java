package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLineDto {

    private Long id;
    private Long purchaseOrderId;
    private Long productId;
    private String productSku;
    private String productName;
    private Long productVariantId;
    private String variantName;
    private Integer lineNumber;
    private String description;
    private BigDecimal quantity;
    private BigDecimal receivedQuantity;
    private BigDecimal cancelledQuantity;
    private BigDecimal pendingQuantity;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private String notes;
    private LocalDate expectedDate;
    private boolean fullyReceived;
}
