package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVendorDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long vendorId;
    private String vendorName;
    private String vendorCode;
    private String vendorSku;
    private String vendorProductName;
    private BigDecimal unitCost;
    private BigDecimal minOrderQuantity;
    private Integer leadTimeDays;
    private boolean preferred;
    private boolean active;
    private String notes;
}
